package com.zst.zsearch.streaming.dlq;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.zst.zsearch.streaming.model.IngestRecord;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DlqPayloadTest {

    @Test
    void includesErrorSourceAndOriginalPayload() {
        IngestRecord record = new IngestRecord();
        record.setRawPayload("{\"articleId\":\"article-1\"}");
        record.setTopic("zsearch.raw.article");
        record.setPartition(3);
        record.setOffset(42L);
        record.setKafkaTimestamp(1710000000000L);
        record.setArticleId("article-1");

        String payload = DlqProducer.payload(record, "VALIDATION_ERROR", "坏数据");

        JSONObject json = JSON.parseObject(payload);
        assertThat(json.getString("errorType")).isEqualTo("VALIDATION_ERROR");
        assertThat(json.getString("errorMessage")).isEqualTo("坏数据");
        assertThat(json.getString("articleId")).isEqualTo("article-1");
        assertThat(json.getString("payload")).isEqualTo(record.getRawPayload());

        JSONObject source = json.getJSONObject("source");
        assertThat(source.getString("topic")).isEqualTo("zsearch.raw.article");
        assertThat(source.getIntValue("partition")).isEqualTo(3);
        assertThat(source.getLongValue("offset")).isEqualTo(42L);
    }

    @Test
    void truncatesLongErrorMessages() {
        IngestRecord record = new IngestRecord();

        JSONObject json = JSON.parseObject(DlqProducer.payload(record, "ES_WRITE_FAILED", "x".repeat(800)));

        assertThat(json.getString("errorMessage")).hasSize(500);
    }
}
