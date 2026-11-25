package com.zst.zsearch.streaming.dlq;

import com.zst.zsearch.streaming.model.IngestRecord;
import org.apache.flink.api.connector.sink2.Sink;
import org.apache.flink.api.connector.sink2.SinkWriter;
import org.apache.flink.metrics.Counter;

import java.io.IOException;

/**
 * 流水线侧输出的 DLQ sink（校验/富化失败）。同步发送，保证错误可见。
 */
public class DlqSink implements Sink<IngestRecord> {

    private final DlqConfig config;

    public DlqSink(DlqConfig config) {
        this.config = config;
    }

    @Override
    public SinkWriter<IngestRecord> createWriter(InitContext context) throws IOException {
        return new DlqWriter(config, context.metricGroup().counter("dlqSent"));
    }

    private static final class DlqWriter implements SinkWriter<IngestRecord> {

        private final DlqProducer producer;
        private final Counter dlqSent;

        private DlqWriter(DlqConfig config, Counter dlqSent) {
            this.producer = new DlqProducer(config);
            this.dlqSent = dlqSent;
        }

        @Override
        public void write(IngestRecord element, Context context) {
            producer.send(element, element.getErrorType(), element.getErrorMessage());
            dlqSent.inc();
        }

        @Override
        public void flush(boolean endOfInput) {
            // 同步发送，无需额外刷新
        }

        @Override
        public void close() {
            producer.close();
        }
    }
}
