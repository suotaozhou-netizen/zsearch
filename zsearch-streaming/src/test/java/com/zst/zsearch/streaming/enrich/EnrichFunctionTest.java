package com.zst.zsearch.streaming.enrich;

import com.zst.zsearch.common.enrich.EnricherChain;
import com.zst.zsearch.common.model.ArticleIndexDocument;
import com.zst.zsearch.common.model.IngestEnvelope;
import com.zst.zsearch.common.model.RawArticle;
import com.zst.zsearch.streaming.model.IngestRecord;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class EnrichFunctionTest {

    @Test
    void buildsDocumentWithEnvelopeVersionAndPipelineMetadata() {
        RawArticle article = new RawArticle();
        article.setUrl("https://example.com/a/1");
        article.setTitle("某企业发生事故");
        article.setText("现场发生火灾，造成 2 人死亡，引发大量投诉和质疑");
        article.setSiteId("4");
        article.setRepostCount(10L);
        article.setCommentCount(20L);
        article.setFollowerCount(5L);
        article.setCollectCount(1L);
        article.setImageList(List.of("https://img/1.jpg"));

        IngestEnvelope envelope = new IngestEnvelope();
        envelope.setArticle(article);

        IngestRecord record = new IngestRecord();
        record.setEnvelope(envelope);
        record.setArticleId("article-1");
        record.setRawVersion(1710000000000L);
        record.setContentHash("hash-1");

        ArticleIndexDocument document = EnrichFunction.buildDocument(
                record, new EnricherChain(12), 7, "2026-11-21T00:00:00+08:00");

        assertThat(document.getId()).isEqualTo("article-1");
        assertThat(document.getRawVersion()).isEqualTo(1710000000000L);
        assertThat(document.getPipelineVersion()).isEqualTo(7);
        assertThat(document.getContentHash()).isEqualTo("hash-1");
        assertThat(document.getIndexedAt()).isEqualTo("2026-11-21T00:00:00+08:00");
        assertThat(document.getTextLength()).isEqualTo(article.getText().length());
        assertThat(document.getInteractionCount()).isEqualTo(36L);
        assertThat(document.getSummary()).isEqualTo(article.getText().substring(0, 12));
        assertThat(document.getEmotion()).isEqualTo("负面");
        assertThat(document.getRiskLevel()).isEqualTo("重点关注");
        assertThat(document.getImageList()).containsExactly("https://img/1.jpg");
    }
}
