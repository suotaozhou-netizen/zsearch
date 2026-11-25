package com.zst.zsearch.streaming.dedup;

import com.zst.zsearch.streaming.model.IngestRecord;
import org.apache.flink.api.common.state.StateTtlConfig;
import org.apache.flink.api.common.state.ValueState;
import org.apache.flink.api.common.state.ValueStateDescriptor;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.metrics.Counter;
import org.apache.flink.streaming.api.functions.KeyedProcessFunction;
import org.apache.flink.util.Collector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;

/**
 * 按 articleId 去重：状态编码为 "contentHash|lastSeenRawVersion"。
 * 去重只是省写入的优化；正确性下线由 ES 外部版本号保证。
 */
public class DedupFunction extends KeyedProcessFunction<String, IngestRecord, IngestRecord> {

    private static final Logger log = LoggerFactory.getLogger(DedupFunction.class);

    public enum Decision {
        PASS,
        DROP_STALE_VERSION,
        DROP_SAME_CONTENT
    }

    private final long ttlDays;

    private transient ValueState<String> dedupState;
    private transient Counter passedCounter;
    private transient Counter droppedCounter;

    public DedupFunction(long ttlDays) {
        this.ttlDays = ttlDays;
    }

    @Override
    public void open(Configuration parameters) {
        StateTtlConfig ttl = StateTtlConfig.newBuilder(Duration.ofDays(ttlDays))
                .setUpdateType(StateTtlConfig.UpdateType.OnCreateAndWrite)
                .setStateVisibility(StateTtlConfig.StateVisibility.NeverReturnExpired)
                .build();
        ValueStateDescriptor<String> descriptor = new ValueStateDescriptor<>("article-dedup", String.class);
        descriptor.enableTimeToLive(ttl);
        dedupState = getRuntimeContext().getState(descriptor);

        passedCounter = getRuntimeContext().getMetricGroup().counter("recordsPassed");
        droppedCounter = getRuntimeContext().getMetricGroup().counter("recordsDeduped");
    }

    @Override
    public void processElement(IngestRecord record, Context ctx, Collector<IngestRecord> out) throws Exception {
        String current = dedupState.value();
        Decision decision = decide(current, record.getRawVersion(), record.getContentHash());
        switch (decision) {
            case PASS -> {
                dedupState.update(encode(record.getContentHash(), record.getRawVersion()));
                passedCounter.inc();
                log.debug("去重通过, articleId={}, rawVersion={}", record.getArticleId(), record.getRawVersion());
                out.collect(record);
            }
            case DROP_SAME_CONTENT -> {
                dedupState.update(encode(record.getContentHash(), record.getRawVersion()));
                droppedCounter.inc();
                log.debug("去重丢弃: 内容未变化, articleId={}, rawVersion={}",
                        record.getArticleId(), record.getRawVersion());
            }
            case DROP_STALE_VERSION -> {
                droppedCounter.inc();
                log.debug("去重丢弃: 版本过旧, articleId={}, rawVersion={}",
                        record.getArticleId(), record.getRawVersion());
            }
        }
    }

    public static Decision decide(String encodedState, long rawVersion, String contentHash) {
        if (encodedState == null) {
            return Decision.PASS;
        }
        int separator = encodedState.lastIndexOf('|');
        if (separator < 0) {
            return Decision.PASS;
        }
        long lastSeenVersion;
        try {
            lastSeenVersion = Long.parseLong(encodedState.substring(separator + 1));
        } catch (NumberFormatException e) {
            return Decision.PASS;
        }
        if (rawVersion <= lastSeenVersion) {
            return Decision.DROP_STALE_VERSION;
        }
        String lastHash = encodedState.substring(0, separator);
        if (lastHash.equals(contentHash)) {
            return Decision.DROP_SAME_CONTENT;
        }
        return Decision.PASS;
    }

    static String encode(String contentHash, long rawVersion) {
        return contentHash + "|" + rawVersion;
    }
}
