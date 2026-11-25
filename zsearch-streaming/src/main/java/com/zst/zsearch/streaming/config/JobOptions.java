package com.zst.zsearch.streaming.config;

import com.zst.zsearch.common.contract.Topics;
import com.zst.zsearch.streaming.dlq.DlqConfig;
import com.zst.zsearch.streaming.sink.EsSinkConfig;
import lombok.Getter;
import org.apache.flink.api.java.utils.ParameterTool;

@Getter
public final class JobOptions {

    private final String kafkaBootstrapServers;
    private final String topic;
    private final String groupId;
    private final String dlqTopic;

    private final String esUris;
    private final String esUsername;
    private final String esPassword;
    private final String esIndex;

    private final int pipelineVersion;
    private final int summaryLength;
    private final int dedupTtlDays;

    private final int batchSize;
    private final long flushIntervalMs;
    private final int maxBufferBytes;
    private final int maxRetries;
    private final long backoffBaseMs;

    private final int parallelism;
    private final String checkpointDir;
    private final Long fromTimestamp;
    private final Long toTimestamp;

    private JobOptions(ParameterTool params) {
        this.kafkaBootstrapServers = params.get("kafka.bootstrap.servers", "8.163.135.98:9092");
        this.topic = params.get("kafka.topic", Topics.RAW_ARTICLE);
        this.groupId = params.get("kafka.group.id", "zsearch-ingest-v1");
        this.dlqTopic = params.get("kafka.dlq.topic", Topics.RAW_ARTICLE_DLQ);

        this.esUris = params.get("es.uris", "http://8.163.135.98:9200");
        this.esUsername = params.get("es.username", "");
        this.esPassword = params.get("es.password", "");
        this.esIndex = params.get("es.index", "zsearch_article");

        this.pipelineVersion = params.getInt("pipeline.version", 1);
        this.summaryLength = params.getInt("summary.length", 120);
        this.dedupTtlDays = params.getInt("dedup.ttl.days", 30);

        this.batchSize = params.getInt("sink.batch.size", 500);
        this.flushIntervalMs = params.getLong("sink.flush.interval.ms", 1000);
        this.maxBufferBytes = params.getInt("sink.max.buffer.bytes", 5 * 1024 * 1024);
        this.maxRetries = params.getInt("sink.max.retries", 5);
        this.backoffBaseMs = params.getLong("sink.backoff.base.ms", 200);

        this.parallelism = params.getInt("parallelism", 1);
        this.checkpointDir = params.get("checkpoint.dir", null);
        this.fromTimestamp = params.has("from.timestamp") ? params.getLong("from.timestamp") : null;
        this.toTimestamp = params.has("to.timestamp") ? params.getLong("to.timestamp") : null;

        if (fromTimestamp != null && toTimestamp != null && fromTimestamp > toTimestamp) {
            throw new IllegalArgumentException("from.timestamp 不能大于 to.timestamp");
        }
    }

    public static JobOptions fromArgs(String[] args) {
        return new JobOptions(ParameterTool.fromArgs(args));
    }

    public EsSinkConfig toEsSinkConfig() {
        EsSinkConfig config = new EsSinkConfig();
        config.setUris(esUris);
        config.setUsername(esUsername);
        config.setPassword(esPassword);
        config.setIndex(esIndex);
        config.setBatchSize(batchSize);
        config.setFlushIntervalMs(flushIntervalMs);
        config.setMaxBufferBytes(maxBufferBytes);
        config.setMaxRetries(maxRetries);
        config.setBackoffBaseMs(backoffBaseMs);
        config.setDlqBootstrapServers(kafkaBootstrapServers);
        config.setDlqTopic(dlqTopic);
        return config;
    }

    public DlqConfig toDlqConfig() {
        DlqConfig config = new DlqConfig();
        config.setBootstrapServers(kafkaBootstrapServers);
        config.setTopic(dlqTopic);
        return config;
    }
}
