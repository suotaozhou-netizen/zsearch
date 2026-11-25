package com.zst.zsearch.streaming.model;

import com.zst.zsearch.common.model.ArticleIndexDocument;
import com.zst.zsearch.common.model.IngestEnvelope;
import lombok.Getter;
import lombok.Setter;
import org.apache.kafka.clients.consumer.ConsumerRecord;

import java.nio.charset.StandardCharsets;

/**
 * 作业内部消息包装：保留 Kafka 定位信息，便于 DLQ 回传原始数据。
 * 必须是标准 POJO（无参构造 + getter/setter），供 Flink 序列化。
 */
@Getter
@Setter
public class IngestRecord {

    private String rawPayload;
    private String topic;
    private int partition;
    private long offset;
    private long kafkaTimestamp;

    private IngestEnvelope envelope;
    private String articleId;
    private long rawVersion;
    private String contentHash;
    private ArticleIndexDocument document;

    private String errorType;
    private String errorMessage;

    public static IngestRecord fromKafka(ConsumerRecord<byte[], byte[]> record) {
        IngestRecord ingest = new IngestRecord();
        ingest.setRawPayload(record.value() == null
                ? null
                : new String(record.value(), StandardCharsets.UTF_8));
        ingest.setTopic(record.topic());
        ingest.setPartition(record.partition());
        ingest.setOffset(record.offset());
        ingest.setKafkaTimestamp(record.timestamp());
        return ingest;
    }
}
