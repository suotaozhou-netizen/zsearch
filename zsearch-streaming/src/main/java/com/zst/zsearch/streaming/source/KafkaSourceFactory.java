package com.zst.zsearch.streaming.source;

import com.zst.zsearch.streaming.config.JobOptions;
import com.zst.zsearch.streaming.model.IngestRecord;
import org.apache.flink.connector.kafka.source.KafkaSource;
import org.apache.flink.connector.kafka.source.KafkaSourceBuilder;
import org.apache.flink.connector.kafka.source.enumerator.initializer.OffsetsInitializer;
import org.apache.kafka.clients.consumer.OffsetResetStrategy;

public final class KafkaSourceFactory {

    private KafkaSourceFactory() {
    }

    /**
     * 常规加工：从已提交 offset 续读，无提交时从头开始。
     * 重建：传入 from/to 时间戳，to 存在时使用有界消费。
     */
    public static KafkaSource<IngestRecord> create(JobOptions options) {
        OffsetsInitializer startingOffsets = options.getFromTimestamp() == null
                ? OffsetsInitializer.committedOffsets(OffsetResetStrategy.EARLIEST)
                : OffsetsInitializer.timestamp(options.getFromTimestamp());

        KafkaSourceBuilder<IngestRecord> builder = KafkaSource.<IngestRecord>builder()
                .setBootstrapServers(options.getKafkaBootstrapServers())
                .setTopics(options.getTopic())
                .setGroupId(options.getGroupId())
                .setStartingOffsets(startingOffsets)
                .setDeserializer(new ArticleRecordDeserializer())
                .setProperty("partition.discovery.interval.ms", "60000")
                .setProperty("enable.auto.commit", "false");

        if (options.getToTimestamp() != null) {
            builder.setBounded(OffsetsInitializer.timestamp(options.getToTimestamp()));
        }
        return builder.build();
    }
}
