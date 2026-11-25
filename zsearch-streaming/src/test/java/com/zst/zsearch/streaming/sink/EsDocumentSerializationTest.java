package com.zst.zsearch.streaming.sink;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.zst.zsearch.common.model.ArticleIndexDocument;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 守护 ES 序列化契约：server 与 Flink 都使用 SNAKE_CASE，个别字段显式驼峰。
 */
class EsDocumentSerializationTest {

    private final ObjectMapper objectMapper = new ObjectMapper()
            .setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);

    @Test
    void serializesFieldNamesMatchingExistingMapping() throws Exception {
        ArticleIndexDocument document = new ArticleIndexDocument();
        document.setId("article-1");
        document.setHomeUrl("https://weibo.com/u/1");
        document.setSiteId("4");
        document.setUserId("u-1");
        document.setImageList(List.of("https://img/1.jpg"));
        document.setVideoList(List.of("https://video/1.mp4"));
        document.setCreatedAt("2026-11-20T20:00:00");
        document.setCrawlerTime("2026-11-20T23:20:00");
        document.setRiskLevel("重点关注");
        document.setTextLength(10);
        document.setInteractionCount(36L);
        document.setRawVersion(1710000000000L);
        document.setPipelineVersion(1);
        document.setContentHash("hash-1");
        document.setIndexedAt("2026-11-21T00:00:00+08:00");

        String json = objectMapper.writeValueAsString(document);
        @SuppressWarnings("unchecked")
        Map<String, Object> fields = objectMapper.readValue(json, Map.class);

        assertThat(fields).containsKeys(
                "id", "homeUrl", "siteId", "userId", "imageList", "videoList",
                "created_at", "crawler_time", "risk_level", "text_length", "interaction_count",
                "raw_version", "pipeline_version", "content_hash", "indexed_at");
        assertThat(fields).doesNotContainKeys("home_url", "site_id", "user_id", "image_list", "video_list");
    }
}
