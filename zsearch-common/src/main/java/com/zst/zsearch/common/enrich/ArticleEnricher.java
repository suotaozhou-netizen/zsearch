package com.zst.zsearch.common.enrich;

import com.zst.zsearch.common.model.ArticleIndexDocument;
import com.zst.zsearch.common.model.RawArticle;

public interface ArticleEnricher {

    void enrich(RawArticle source, ArticleIndexDocument target);
}
