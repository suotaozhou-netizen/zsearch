package com.zst.zsearch.streaming;

import com.zst.zsearch.streaming.adapter.CollectorEventAdapter;
import com.zst.zsearch.streaming.config.JobOptions;
import com.zst.zsearch.streaming.dedup.DedupFunction;
import com.zst.zsearch.streaming.dlq.DlqSink;
import com.zst.zsearch.streaming.enrich.EnrichFunction;
import com.zst.zsearch.streaming.model.IngestRecord;
import com.zst.zsearch.streaming.sink.EsBulkSink;
import com.zst.zsearch.streaming.source.KafkaSourceFactory;
import com.zst.zsearch.streaming.validate.ArticleValidateFunction;
import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.api.common.restartstrategy.RestartStrategies;
import org.apache.flink.api.common.time.Time;
import org.apache.flink.configuration.ExternalizedCheckpointRetention;
import org.apache.flink.runtime.state.hashmap.HashMapStateBackend;
import org.apache.flink.streaming.api.CheckpointingMode;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
import org.apache.flink.streaming.api.environment.CheckpointConfig;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class IngestJob {

    private static final Logger log = LoggerFactory.getLogger(IngestJob.class);

    private IngestJob() {
    }

    public static void main(String[] args) throws Exception {
        JobOptions options = JobOptions.fromArgs(args);
        log.info("启动 zsearch-ingest: kafka={}, topic={}, group={}, es={}, index={}, parallelism={}, checkpoint={}",
                options.getKafkaBootstrapServers(), options.getTopic(), options.getGroupId(),
                options.getEsUris(), options.getEsIndex(), options.getParallelism(), options.getCheckpointDir());

        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        env.setParallelism(options.getParallelism());
        configureCheckpointing(env, options);

        DataStream<IngestRecord> source = env
                .fromSource(KafkaSourceFactory.create(options),
                        WatermarkStrategy.noWatermarks(), "kafka-source")
                .uid("kafka-source");

        SingleOutputStreamOperator<IngestRecord> adapted = source
                .process(new CollectorEventAdapter())
                .name("collector-adapter")
                .uid("collector-adapter");

        SingleOutputStreamOperator<IngestRecord> validated = adapted
                .process(new ArticleValidateFunction())
                .name("validate")
                .uid("validate");

        SingleOutputStreamOperator<IngestRecord> deduped = validated
                .keyBy(IngestRecord::getArticleId)
                .process(new DedupFunction(options.getDedupTtlDays()))
                .name("dedup")
                .uid("dedup");

        SingleOutputStreamOperator<IngestRecord> enriched = deduped
                .process(new EnrichFunction(options.getSummaryLength(), options.getPipelineVersion()))
                .name("enrich")
                .uid("enrich");

        DataStream<IngestRecord> dlq = adapted.getSideOutput(CollectorEventAdapter.DLQ_TAG)
                .union(validated.getSideOutput(ArticleValidateFunction.DLQ_TAG),
                        enriched.getSideOutput(EnrichFunction.DLQ_TAG));

        enriched.sinkTo(new EsBulkSink(options.toEsSinkConfig()))
                .name("es-sink")
                .uid("es-sink");

        dlq.sinkTo(new DlqSink(options.toDlqConfig()))
                .name("dlq-sink")
                .uid("dlq-sink");

        env.execute("zsearch-ingest");
    }

    private static void configureCheckpointing(StreamExecutionEnvironment env, JobOptions options) {
        env.enableCheckpointing(60_000, CheckpointingMode.AT_LEAST_ONCE);
        CheckpointConfig checkpoint = env.getCheckpointConfig();
        checkpoint.setMinPauseBetweenCheckpoints(30_000);
        checkpoint.setCheckpointTimeout(600_000);
        checkpoint.setMaxConcurrentCheckpoints(1);
        checkpoint.setExternalizedCheckpointRetention(ExternalizedCheckpointRetention.RETAIN_ON_CANCELLATION);
        if (options.getCheckpointDir() != null && !options.getCheckpointDir().isBlank()) {
            checkpoint.setCheckpointStorage(options.getCheckpointDir());
        }

        env.setStateBackend(new HashMapStateBackend());
        env.setRestartStrategy(RestartStrategies.fixedDelayRestart(3, Time.seconds(10)));
    }
}
