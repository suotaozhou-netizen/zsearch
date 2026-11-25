package com.zst.zsearch.streaming.model;

import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.api.java.typeutils.PojoTypeInfo;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 防止 Flink 退化为 Kryo 序列化：IngestRecord 及其嵌套对象必须满足 POJO 规则。
 */
class IngestRecordTypeTest {

    @Test
    void isRecognizedAsPojo() {
        TypeInformation<IngestRecord> typeInfo = TypeInformation.of(IngestRecord.class);

        assertThat(typeInfo).isInstanceOf(PojoTypeInfo.class);
    }
}
