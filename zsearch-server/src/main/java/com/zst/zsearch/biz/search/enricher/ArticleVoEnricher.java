package com.zst.zsearch.biz.search.enricher;

import com.zst.zsearch.biz.model.vo.ArticleVo;

import java.util.List;

public interface ArticleVoEnricher {

    void enrich(List<ArticleVo> articles, ArticleVoEnrichContext context);
}
