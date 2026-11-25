package com.zst.zsearch.streaming.sink;

import com.zst.zsearch.streaming.model.IngestRecord;
import org.apache.flink.api.connector.sink2.Sink;
import org.apache.flink.api.connector.sink2.SinkWriter;

import java.io.IOException;

/**
 * 自研 ES sink：必须携带 external_gte 版本号，禁止使用 flink-connector-elasticsearch7。
 */
public class EsBulkSink implements Sink<IngestRecord> {

    private final EsSinkConfig config;

    public EsBulkSink(EsSinkConfig config) {
        this.config = config;
    }

    @Override
    public SinkWriter<IngestRecord> createWriter(InitContext context) throws IOException {
        return new EsBulkWriter(config, context.metricGroup());
    }
}
