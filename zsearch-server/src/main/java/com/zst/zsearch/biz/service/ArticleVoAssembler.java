package com.zst.zsearch.biz.service;

import com.zst.zsearch.biz.search.converter.ArticleVoConverter;
import com.zst.zsearch.biz.search.enricher.ArticleVoEnrichContext;
import com.zst.zsearch.biz.search.enricher.ArticleVoEnricher;
import com.zst.zsearch.biz.model.vo.ArticleListVo;
import com.zst.zsearch.biz.model.vo.ArticleVo;
import com.zst.zsearch.es.dto.Article;
import com.zst.zsearch.es.dto.ArticleSearchResponse;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ArticleVoAssembler {

    private final ArticleVoConverter converter;
    private final List<ArticleVoEnricher> enrichers;

    public ArticleVoAssembler(ArticleVoConverter converter, List<ArticleVoEnricher> enrichers) {
        this.converter = converter;
        this.enrichers = List.copyOf(enrichers);
    }

    public ArticleListVo assemble(ArticleSearchResponse response) {
        return assemble(response, ArticleVoEnrichContext.empty());
    }

    public ArticleVo assemble(Article article) {
        return assemble(article, ArticleVoEnrichContext.empty());
    }

    public ArticleVo assemble(Article article, ArticleVoEnrichContext context) {
        ArticleVo result = converter.toVo(article);
        List<ArticleVo> articles = List.of(result);
        ArticleVoEnrichContext effectiveContext =
                context == null ? ArticleVoEnrichContext.empty() : context;
        for (ArticleVoEnricher enricher : enrichers) {
            enricher.enrich(articles, effectiveContext);
        }
        return result;
    }

    public ArticleListVo assemble(ArticleSearchResponse response, ArticleVoEnrichContext context) {
        ArticleListVo result = converter.toListVo(response);
        List<ArticleVo> articles = result.getArticleList();
        if (articles.isEmpty()) {
            return result;
        }
        ArticleVoEnrichContext effectiveContext =
                context == null ? ArticleVoEnrichContext.empty() : context;
        for (ArticleVoEnricher enricher : enrichers) {
            enricher.enrich(articles, effectiveContext);
        }
        return result;
    }
}
