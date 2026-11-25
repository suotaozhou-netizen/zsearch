package com.zst.zsearch.streaming.validate;

import com.zst.zsearch.common.id.ContentFingerprint;
import com.zst.zsearch.common.json.JsonCodec;
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

public class ArticleValidateFunction extends ProcessFunction<IngestRecord, IngestRecord> {

    private static final Logger log = LoggerFactory.getLogger(ArticleValidateFunction.class);

    public static final String PARSE_ERROR = "PARSE_ERROR";
    public static final String EMPTY_PAYLOAD = "EMPTY_PAYLOAD";

    public static final OutputTag<IngestRecord> DLQ_TAG =
            new OutputTag<>("validate-dlq", TypeInformation.of(IngestRecord.class));

    private transient Counter validCounter;
    private transient Counter invalidCounter;

    @Override
    public void open(Configuration parameters) {
        validCounter = getRuntimeContext().getMetricGroup().counter("recordsValidated");
        invalidCounter = getRuntimeContext().getMetricGroup().counter("recordsInvalid");
    }

    @Override
    public void processElement(IngestRecord record, Context ctx, Collector<IngestRecord> out) {
        if (record.getRawPayload() == null || record.getRawPayload().isBlank()) {
            reject(record, EMPTY_PAYLOAD, "Kafka 消息为空", ctx);
            return;
        }

        IngestEnvelope envelope = record.getEnvelope();
        if (envelope == null) {
            try {
                envelope = JsonCodec.parse(record.getRawPayload(), IngestEnvelope.class);
            } catch (RuntimeException e) {
                reject(record, PARSE_ERROR, "消息解析失败: " + e.getMessage(), ctx);
                return;
            }
        }

        ArticleValidator.Error error = ArticleValidator.validate(envelope);
        if (error != null) {
            reject(record, error.type(), error.message(), ctx);
            return;
        }

        record.setEnvelope(envelope);
        record.setArticleId(envelope.getArticleId());
        record.setRawVersion(envelope.getRawVersion());
        record.setContentHash(ContentFingerprint.of(envelope.getArticle()));
        record.setErrorType(null);
        record.setErrorMessage(null);
        validCounter.inc();
        log.debug("消息校验通过, articleId={}, rawVersion={}, partition={}, offset={}",
                record.getArticleId(), record.getRawVersion(), record.getPartition(), record.getOffset());
        out.collect(record);
    }

    private void reject(IngestRecord record, String type, String message, Context ctx) {
        record.setErrorType(type);
        record.setErrorMessage(message);
        invalidCounter.inc();
        log.warn("消息校验失败, type={}, topic={}, partition={}, offset={}, message={}",
                type, record.getTopic(), record.getPartition(), record.getOffset(), message);
        ctx.output(DLQ_TAG, record);
    }
}
