package com.zst.zsearch.common.enrich;

import com.zst.zsearch.common.model.ArticleIndexDocument;
import com.zst.zsearch.common.model.RawArticle;

public class TextLengthEnricher implements ArticleEnricher {

    @Override
    public void enrich(RawArticle source, ArticleIndexDocument target) {
        target.setTextLength(TextSupport.hasText(source.getText()) ? source.getText().length() : 0);
    }
}
