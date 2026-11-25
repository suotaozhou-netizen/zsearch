package com.zst.zsearch.biz.search.enricher;

import com.zst.zsearch.biz.model.vo.ArticleVo;
import com.zst.zsearch.util.KeywordHighlighter;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
@Order(100)
public class KeywordHighlightEnricher implements ArticleVoEnricher {

    @Override
    public void enrich(List<ArticleVo> articles, ArticleVoEnrichContext context) {
        if (articles.isEmpty() || context.keywords().isEmpty()) {
            return;
        }
        List<String> terms = KeywordHighlighter.parseTerms(context.keywords());
        if (terms.isEmpty()) {
            return;
        }
        for (ArticleVo article : articles) {
            Map<String, Integer> highlight = KeywordHighlighter.countHits(
                    terms, article.getTitle(), article.getText());
            article.setHighlight(highlight);
        }
    }
}
