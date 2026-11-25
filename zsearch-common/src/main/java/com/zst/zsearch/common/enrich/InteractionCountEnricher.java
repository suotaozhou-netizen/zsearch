package com.zst.zsearch.common.enrich;

import com.zst.zsearch.common.model.ArticleIndexDocument;
import com.zst.zsearch.common.model.RawArticle;

public class InteractionCountEnricher implements ArticleEnricher {

    @Override
    public void enrich(RawArticle source, ArticleIndexDocument target) {
        long total = valueOf(source.getRepostCount())
                + valueOf(source.getCommentCount())
                + valueOf(source.getLikeCount())
                + valueOf(source.getFollowerCount())
                + valueOf(source.getCollectCount());
        target.setInteractionCount(total);
    }

    private long valueOf(Long value) {
        return value == null ? 0L : value;
    }
}
