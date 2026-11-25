package com.zst.zsearch.common.enrich;

import com.zst.zsearch.common.model.ArticleIndexDocument;
import com.zst.zsearch.common.model.RawArticle;

import java.util.List;

/**
 * 固定顺序的富化链：先复制原始字段，再依次执行纯规则。
 * 顺序是契约的一部分，调整顺序会改变产出结果。
 */
public final class EnricherChain {

    public static final int DEFAULT_SUMMARY_LENGTH = 120;

    private final List<ArticleEnricher> enrichers;

    public EnricherChain(int summaryLength) {
        this(defaults(summaryLength));
    }

    public EnricherChain(List<ArticleEnricher> enrichers) {
        this.enrichers = List.copyOf(enrichers);
    }

    public static List<ArticleEnricher> defaults(int summaryLength) {
        return List.of(
                new TextLengthEnricher(),
                new InteractionCountEnricher(),
                new SummaryEnricher(summaryLength),
                new EmotionEnricher(),
                new RiskLevelEnricher());
    }

    public ArticleIndexDocument assemble(String articleId, RawArticle raw) {
        ArticleIndexDocument document = new ArticleIndexDocument();
        document.setId(articleId);
        document.setTitle(raw.getTitle());
        document.setText(raw.getText());
        document.setSummary(raw.getSummary());
        document.setName(raw.getName());
        document.setSource(raw.getSource());
        document.setUrl(raw.getUrl());
        document.setHomeUrl(raw.getHomeUrl());
        document.setAvatar(raw.getAvatar());
        document.setCreatedAt(raw.getCreatedAt());
        document.setCrawlerTime(raw.getCrawlerTime());
        document.setEmotion(raw.getEmotion());
        document.setRiskLevel(raw.getRiskLevel());
        document.setOcr(raw.getOcr());
        document.setAsr(raw.getAsr());
        document.setRepostCount(raw.getRepostCount());
        document.setCommentCount(raw.getCommentCount());
        document.setLikeCount(raw.getLikeCount());
        document.setFollowerCount(raw.getFollowerCount());
        document.setCollectCount(raw.getCollectCount());
        document.setSiteId(raw.getSiteId());
        document.setUserId(raw.getUserId());
        document.setImageList(raw.getImageList());
        document.setVideoList(raw.getVideoList());
        document.setRawVersion(raw.getRawVersion());

        for (ArticleEnricher enricher : enrichers) {
            enricher.enrich(raw, document);
        }
        return document;
    }
}
