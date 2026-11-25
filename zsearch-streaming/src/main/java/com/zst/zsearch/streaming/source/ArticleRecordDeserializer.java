package com.zst.zsearch.streaming.source;

import com.zst.zsearch.streaming.model.IngestRecord;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.connector.kafka.source.reader.deserializer.KafkaRecordDeserializationSchema;
import org.apache.flink.util.Collector;
import org.apache.kafka.clients.consumer.ConsumerRecord;

import java.io.IOException;

/**
 * 记录级反序列化器：只做字节到字符串与元数据包装，解析留给校验算子，便于 DLQ 原始回传。
 */
public class ArticleRecordDeserializer implements KafkaRecordDeserializationSchema<IngestRecord> {

    @Override
    public void deserialize(ConsumerRecord<byte[], byte[]> record, Collector<IngestRecord> out) throws IOException {
        out.collect(IngestRecord.fromKafka(record));
    }

    @Override
    public TypeInformation<IngestRecord> getProducedType() {
        return TypeInformation.of(IngestRecord.class);
    }
}
