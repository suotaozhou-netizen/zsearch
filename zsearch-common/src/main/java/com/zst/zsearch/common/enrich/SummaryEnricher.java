package com.zst.zsearch.common.enrich;

import com.zst.zsearch.common.model.ArticleIndexDocument;
import com.zst.zsearch.common.model.RawArticle;

public class SummaryEnricher implements ArticleEnricher {

    private final int maxLength;

    public SummaryEnricher(int maxLength) {
        this.maxLength = maxLength;
    }

    @Override
    public void enrich(RawArticle source, ArticleIndexDocument target) {
        if (TextSupport.hasText(target.getSummary()) || !TextSupport.hasText(source.getText())) {
            return;
        }
        String normalized = source.getText().replaceAll("\\s+", " ").trim();
        target.setSummary(normalized.length() > maxLength
                ? normalized.substring(0, maxLength)
                : normalized);
    }
}
