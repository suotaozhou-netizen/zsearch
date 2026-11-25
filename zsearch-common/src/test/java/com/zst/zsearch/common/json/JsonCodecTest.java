package com.zst.zsearch.common.json;

import com.zst.zsearch.common.contract.SchemaVersion;
import com.zst.zsearch.common.model.IngestEnvelope;
import com.zst.zsearch.common.model.RawArticle;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class JsonCodecTest {

    @Test
    void roundTripsIngestEnvelope() {
        RawArticle article = new RawArticle();
        article.setUrl("https://www.xiaohongshu.com/explore/abc123");
        article.setTitle("标题");
        article.setText("正文");
        article.setSiteId("2");
        article.setLikeCount(10L);

        IngestEnvelope envelope = new IngestEnvelope();
        envelope.setSchemaVersion(SchemaVersion.CURRENT);
        envelope.setIdVersion(SchemaVersion.ARTICLE_ID_V1);
        envelope.setArticleId("article-1");
        envelope.setRawVersion(1710000000000L);
        envelope.setCrawledAt("2026-01-01T00:00:00.000+08:00");
        envelope.setArticle(article);

        String json = JsonCodec.toJson(envelope);
        IngestEnvelope parsed = JsonCodec.parse(json, IngestEnvelope.class);

        assertThat(parsed.getSchemaVersion()).isEqualTo(SchemaVersion.CURRENT);
        assertThat(parsed.getIdVersion()).isEqualTo(SchemaVersion.ARTICLE_ID_V1);
        assertThat(parsed.getArticleId()).isEqualTo("article-1");
        assertThat(parsed.getRawVersion()).isEqualTo(1710000000000L);
        assertThat(parsed.getCrawledAt()).isEqualTo("2026-01-01T00:00:00.000+08:00");
        assertThat(parsed.getArticle().getUrl()).isEqualTo(article.getUrl());
        assertThat(parsed.getArticle().getLikeCount()).isEqualTo(10L);
    }
}
