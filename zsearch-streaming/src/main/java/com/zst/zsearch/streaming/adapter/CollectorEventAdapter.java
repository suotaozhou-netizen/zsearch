package com.zst.zsearch.streaming.adapter;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.zst.zsearch.common.contract.SchemaVersion;
import com.zst.zsearch.common.id.ArticleIdGenerator;
import com.zst.zsearch.common.model.IngestEnvelope;
import com.zst.zsearch.streaming.model.IngestRecord;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.metrics.Counter;
import org.apache.flink.streaming.api.functions.ProcessFunction;
import org.apache.flink.util.Collector;
import org.apache.flink.util.OutputTag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 采集端原生事件 {eventId,platform,dataType,crawledAt,payload} 到 IngestEnvelope 的适配层。
 * 无法识别为采集事件的消息原样交给校验器，保持既有信封契约可用。
 */
public class CollectorEventAdapter extends ProcessFunction<IngestRecord, IngestRecord> {

    private static final Logger log = LoggerFactory.getLogger(CollectorEventAdapter.class);

    public static final OutputTag<IngestRecord> DLQ_TAG =
            new OutputTag<>("adapter-dlq", TypeInformation.of(IngestRecord.class));

    private transient Counter adaptedCounter;
    private transient Counter skippedCounter;
    private transient Counter failedCounter;

    @Override
    public void open(Configuration parameters) {
        adaptedCounter = getRuntimeContext().getMetricGroup().counter("recordsAdapted");
        skippedCounter = getRuntimeContext().getMetricGroup().counter("recordsSkippedByAdapter");
        failedCounter = getRuntimeContext().getMetricGroup().counter("recordsAdapterFailed");
    }

    @Override
    public void processElement(IngestRecord record, Context ctx, Collector<IngestRecord> out) {
        String raw = record.getRawPayload();
        if (raw == null || raw.isBlank()) {
            out.collect(record);
            return;
        }

        JSONObject root;
        try {
            root = JSON.parseObject(raw);
        } catch (RuntimeException e) {
            out.collect(record);
            return;
        }
        if (root == null || !(root.get("payload") instanceof JSONObject payload) || root.getString("dataType") == null) {
            out.collect(record);
            return;
        }

        String dataType = root.getString("dataType");
        if (!"content".equalsIgnoreCase(dataType)) {
            skippedCounter.inc();
            log.debug("适配层跳过非文章事件, platform={}, dataType={}, eventId={}, offset={}",
                    root.getString("platform"), dataType, root.getString("eventId"), record.getOffset());
            return;
        }

        try {
            CollectorArticleMapper.Mapped mapped = CollectorArticleMapper.map(
                    root.getString("platform"), payload, root.getLong("crawledAt"), record.getKafkaTimestamp());

            IngestEnvelope envelope = new IngestEnvelope();
            envelope.setSchemaVersion(SchemaVersion.CURRENT);
            envelope.setIdVersion(SchemaVersion.ARTICLE_ID_V1);
            envelope.setArticleId(ArticleIdGenerator.fromUrl(mapped.article().getUrl()));
            envelope.setRawVersion(mapped.rawVersion());
            envelope.setCrawledAt(mapped.crawledAt());
            envelope.setArticle(mapped.article());

            record.setEnvelope(envelope);
            record.setArticleId(envelope.getArticleId());
            record.setRawVersion(envelope.getRawVersion());
            adaptedCounter.inc();
            log.debug("采集事件适配完成, articleId={}, platform={}, offset={}",
                    envelope.getArticleId(), root.getString("platform"), record.getOffset());
            out.collect(record);
        } catch (CollectorArticleMapper.MappingException e) {
            reject(record, root.getString("platform"), e.type(), e.getMessage(), ctx);
        } catch (IllegalArgumentException e) {
            reject(record, root.getString("platform"), CollectorArticleMapper.PAYLOAD_INVALID, e.getMessage(), ctx);
        }
    }

    private void reject(IngestRecord record, String platform, String type, String message, Context ctx) {
        failedCounter.inc();
        record.setErrorType(type);
        record.setErrorMessage("采集事件适配失败: " + message);
        log.warn("采集事件适配失败, type={}, platform={}, topic={}, partition={}, offset={}, message={}",
                type, platform, record.getTopic(), record.getPartition(), record.getOffset(), message);
        ctx.output(DLQ_TAG, record);
    }
}
