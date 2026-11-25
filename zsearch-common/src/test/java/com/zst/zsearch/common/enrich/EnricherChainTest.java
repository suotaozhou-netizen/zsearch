package com.zst.zsearch.common.enrich;

import com.zst.zsearch.common.model.ArticleIndexDocument;
import com.zst.zsearch.common.model.RawArticle;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class EnricherChainTest {

    private final EnricherChain chain = new EnricherChain(12);

    @Test
    void copiesFieldsAndComputesDerivedValues() {
        RawArticle raw = new RawArticle();
        raw.setTitle("某企业发生事故");
        raw.setText("现场发生火灾，造成 2 人死亡，引发大量投诉和质疑");
        raw.setSource("微博");
        raw.setSiteId("4");
        raw.setUserId("u-1");
        raw.setAvatar("https://img.example.com/avatar.jpg");
        raw.setImageList(List.of("https://img/1.jpg"));
        raw.setRawVersion(1710000000000L);
        raw.setRepostCount(10L);
        raw.setCommentCount(20L);
        raw.setLikeCount(null);
        raw.setFollowerCount(5L);
        raw.setCollectCount(1L);

        ArticleIndexDocument document = chain.assemble("article-1", raw);

        assertThat(document.getId()).isEqualTo("article-1");
        assertThat(document.getTitle()).isEqualTo(raw.getTitle());
        assertThat(document.getSource()).isEqualTo("微博");
        assertThat(document.getSiteId()).isEqualTo("4");
        assertThat(document.getUserId()).isEqualTo("u-1");
        assertThat(document.getAvatar()).isEqualTo("https://img.example.com/avatar.jpg");
        assertThat(document.getImageList()).containsExactly("https://img/1.jpg");
        assertThat(document.getRawVersion()).isEqualTo(1710000000000L);
        assertThat(document.getTextLength()).isEqualTo(raw.getText().length());
        assertThat(document.getInteractionCount()).isEqualTo(36L);
        assertThat(document.getSummary()).isEqualTo(raw.getText().substring(0, 12));
        assertThat(document.getEmotion()).isEqualTo("负面");
        assertThat(document.getRiskLevel()).isEqualTo("重点关注");
    }

    @Test
    void keepsCollectedValuesAndSkipsSummaryWhenPresent() {
        RawArticle raw = new RawArticle();
        raw.setTitle("普通新闻");
        raw.setText("这是一段足够长的正文，本来可以被截断成摘要");
        raw.setSummary("采集方提供的摘要");
        raw.setEmotion("正面");
        raw.setRiskLevel("关注风险");

        ArticleIndexDocument document = new EnricherChain(5).assemble("article-2", raw);

        assertThat(document.getSummary()).isEqualTo("采集方提供的摘要");
        assertThat(document.getEmotion()).isEqualTo("正面");
        assertThat(document.getRiskLevel()).isEqualTo("关注风险");
    }

    @Test
    void leavesSummaryEmptyWhenTextMissing() {
        RawArticle raw = new RawArticle();
        raw.setTitle("只有标题");

        ArticleIndexDocument document = new EnricherChain(10).assemble("article-3", raw);

        assertThat(document.getSummary()).isNull();
        assertThat(document.getTextLength()).isZero();
        assertThat(document.getInteractionCount()).isZero();
    }

    @Test
    void doesNotSetPipelineMetadata() {
        RawArticle raw = new RawArticle();
        raw.setTitle("标题");
        raw.setText("正文");

        ArticleIndexDocument document = chain.assemble("article-4", raw);

        assertThat(document.getPipelineVersion()).isNull();
        assertThat(document.getContentHash()).isNull();
        assertThat(document.getIndexedAt()).isNull();
    }
}
