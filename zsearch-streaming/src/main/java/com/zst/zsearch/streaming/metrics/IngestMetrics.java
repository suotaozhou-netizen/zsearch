package com.zst.zsearch.streaming.metrics;

import lombok.Getter;
import org.apache.flink.metrics.Counter;
import org.apache.flink.metrics.MetricGroup;

/**
 * ES sink 运行指标。实例在 TaskManager 上创建，不做序列化。
 */
@Getter
public final class IngestMetrics {

    private final Counter recordsIn;
    private final Counter docsWritten;
    private final Counter versionConflicts;
    private final Counter bulkRetries;
    private final Counter dlqSent;
    private final Counter flushFailures;

    private IngestMetrics(MetricGroup group) {
        this.recordsIn = group.counter("recordsIn");
        this.docsWritten = group.counter("docsWritten");
        this.versionConflicts = group.counter("versionConflicts");
        this.bulkRetries = group.counter("bulkRetries");
        this.dlqSent = group.counter("dlqSent");
        this.flushFailures = group.counter("flushFailures");
    }

    public static IngestMetrics of(MetricGroup group) {
        return new IngestMetrics(group);
    }
}
