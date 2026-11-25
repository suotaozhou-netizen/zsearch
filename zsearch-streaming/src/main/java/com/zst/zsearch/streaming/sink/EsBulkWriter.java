package com.zst.zsearch.streaming.sink;

import co.elastic.clients.elasticsearch._types.ElasticsearchException;
import co.elastic.clients.elasticsearch._types.Refresh;
import co.elastic.clients.elasticsearch._types.VersionType;
import co.elastic.clients.elasticsearch.core.BulkRequest;
import co.elastic.clients.elasticsearch.core.BulkResponse;
import co.elastic.clients.elasticsearch.core.bulk.BulkResponseItem;
import com.zst.zsearch.streaming.dlq.DlqProducer;
import com.zst.zsearch.streaming.metrics.IngestMetrics;
import com.zst.zsearch.streaming.model.IngestRecord;
import org.apache.flink.api.connector.sink2.SinkWriter;
import org.apache.flink.metrics.MetricGroup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 批量写入 ES：_id=articleId，_version=rawVersion，external_gte。
 * 409 视为成功；429/502/503 退避重试；其余错误写 DLQ。刷新交给 Flink checkpoint。
 */
public class EsBulkWriter implements SinkWriter<IngestRecord> {

    private static final Logger log = LoggerFactory.getLogger(EsBulkWriter.class);

    private static final String DLQ_BULK_REJECTED = "ES_BULK_REJECTED";
    private static final String DLQ_RETRY_EXHAUSTED = "ES_RETRY_EXHAUSTED";
    private static final String DLQ_DOCUMENT_MISSING = "ES_DOCUMENT_MISSING";

    private final EsSinkConfig config;
    private final IngestMetrics metrics;
    private final EsClientFactory.EsClientHolder esClient;
    private final DlqProducer dlq;
    private final List<IngestRecord> buffer = new ArrayList<>();

    private long bufferedBytes;
    private long lastFlushAt;

    public EsBulkWriter(EsSinkConfig config, MetricGroup metricGroup) {
        this.config = config;
        this.metrics = IngestMetrics.of(metricGroup);
        this.esClient = EsClientFactory.create(config);
        this.dlq = new DlqProducer(config.toDlqConfig());
        this.lastFlushAt = System.currentTimeMillis();
    }

    @Override
    public void write(IngestRecord element, Context context) throws IOException, InterruptedException {
        if (element.getDocument() == null) {
            sendToDlq(element, DLQ_DOCUMENT_MISSING, "文档为空，无法写入");
            return;
        }
        metrics.getRecordsIn().inc();
        buffer.add(element);
        bufferedBytes += element.getRawPayload() == null ? 0 : element.getRawPayload().length();

        long now = System.currentTimeMillis();
        if (buffer.size() >= config.getBatchSize()
                || bufferedBytes >= config.getMaxBufferBytes()
                || now - lastFlushAt >= config.getFlushIntervalMs()) {
            flushBuffer();
        }
    }

    @Override
    public void flush(boolean endOfInput) throws IOException, InterruptedException {
        flushBuffer();
    }

    private void flushBuffer() throws InterruptedException {
        if (buffer.isEmpty()) {
            lastFlushAt = System.currentTimeMillis();
            return;
        }

        List<IngestRecord> pending = new ArrayList<>(buffer);
        buffer.clear();
        bufferedBytes = 0;

        int totalDocs = pending.size();
        long startedAt = System.currentTimeMillis();
        long dlqBefore = metrics.getDlqSent().getCount();
        int retryRound = 0;
        while (!pending.isEmpty()) {
            BulkResponse response;
            try {
                response = esClient.client().bulk(buildRequest(pending));
            } catch (IOException | ElasticsearchException e) {
                metrics.getFlushFailures().inc();
                if (retryRound == 0) {
                    log.warn("ES 批量写入异常, index={}, docs={}, error={}",
                            config.getIndex(), pending.size(), e.getMessage());
                } else {
                    log.debug("ES 批量写入再次异常, index={}, docs={}, round={}, error={}",
                            config.getIndex(), pending.size(), retryRound, e.getMessage());
                }
                if (retryRound >= config.getMaxRetries()) {
                    pending.forEach(record -> sendToDlq(record, DLQ_RETRY_EXHAUSTED,
                            "ES 写入失败: " + e.getMessage()));
                    break;
                }
                backoff(++retryRound);
                continue;
            }

            pending = applyResponse(pending, response);
            if (!pending.isEmpty()) {
                if (retryRound == 0) {
                    log.warn("ES 部分文档待重试, index={}, docs={}", config.getIndex(), pending.size());
                }
                if (retryRound >= config.getMaxRetries()) {
                    pending.forEach(record -> sendToDlq(record, DLQ_RETRY_EXHAUSTED, "ES 重试次数用尽"));
                    break;
                }
                backoff(++retryRound);
            }
        }
        log.info("ES 批量写入完成, index={}, docs={}, dlq={}, retries={}, durationMs={}",
                config.getIndex(), totalDocs, metrics.getDlqSent().getCount() - dlqBefore,
                retryRound, System.currentTimeMillis() - startedAt);
        lastFlushAt = System.currentTimeMillis();
    }

    private BulkRequest buildRequest(List<IngestRecord> records) {
        BulkRequest.Builder builder = new BulkRequest.Builder().refresh(Refresh.False);
        for (IngestRecord record : records) {
            builder.operations(op -> op.index(index -> index
                    .index(config.getIndex())
                    .id(record.getArticleId())
                    .version(record.getRawVersion())
                    .versionType(VersionType.ExternalGte)
                    .document(record.getDocument())));
        }
        return builder.build();
    }

    private List<IngestRecord> applyResponse(List<IngestRecord> records, BulkResponse response) {
        List<BulkResponseItem> items = response.items();
        List<IngestRecord> retry = new ArrayList<>();
        for (int i = 0; i < records.size(); i++) {
            IngestRecord record = records.get(i);
            BulkResponseItem item = i < items.size() ? items.get(i) : null;
            if (item == null) {
                sendToDlq(record, DLQ_BULK_REJECTED, "Elasticsearch 未返回写入结果");
                continue;
            }
            if (item.error() == null) {
                metrics.getDocsWritten().inc();
                log.debug("ES 文档写入成功, articleId={}, version={}",
                        record.getArticleId(), record.getRawVersion());
                continue;
            }
            int status = item.status();
            if (status == 409) {
                // 旧版本被 external_gte 拒绝，文档已是更新版本，视为成功
                metrics.getVersionConflicts().inc();
                log.debug("ES 版本冲突视为成功, articleId={}, version={}",
                        record.getArticleId(), record.getRawVersion());
            } else if (isRetryableStatus(status)) {
                retry.add(record);
            } else {
                sendToDlq(record, DLQ_BULK_REJECTED,
                        "ES 拒绝写入(status=" + status + "): " + errorReason(item));
            }
        }
        return retry;
    }

    static boolean isRetryableStatus(int status) {
        return status == 429 || status == 502 || status == 503;
    }

    private static String errorReason(BulkResponseItem item) {
        if (item.error() == null || item.error().reason() == null) {
            return "unknown";
        }
        return item.error().reason();
    }

    private void backoff(int retryRound) throws InterruptedException {
        metrics.getBulkRetries().inc();
        long base = Math.max(1, config.getBackoffBaseMs());
        long delay = base * (1L << Math.min(retryRound - 1, 5));
        long jitter = ThreadLocalRandom.current().nextLong(Math.max(1, delay / 2));
        Thread.sleep(delay + jitter);
    }

    private void sendToDlq(IngestRecord record, String errorType, String errorMessage) {
        log.error("文档进入 DLQ, articleId={}, type={}, message={}",
                record.getArticleId(), errorType, errorMessage);
        dlq.send(record, errorType, errorMessage);
        metrics.getDlqSent().inc();
    }

    @Override
    public void close() throws Exception {
        try {
            flushBuffer();
        } finally {
            try {
                dlq.close();
            } finally {
                esClient.close();
            }
        }
    }
}
