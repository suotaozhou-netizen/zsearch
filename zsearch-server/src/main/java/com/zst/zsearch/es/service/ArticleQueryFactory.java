package com.zst.zsearch.es.service;

import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.json.JsonData;
import com.zst.zsearch.es.dto.ArticleFilterCondition;
import com.zst.zsearch.es.dto.ArticleSearchCondition;
import com.zst.zsearch.es.dto.ArticleSearchRequest;
import com.zst.zsearch.es.query.EsSearchScope;
import com.zst.zsearch.es.query.FieldTermFilter;
import com.zst.zsearch.es.query.SearchExpressionQueryBuilder;
import com.zst.zsearch.util.SearchExpressionParser;
import org.springframework.http.HttpStatus;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

final class ArticleQueryFactory {

    private ArticleQueryFactory() {
    }

    static BoolQuery build(ArticleSearchRequest request) {
        BoolQuery.Builder bool = new BoolQuery.Builder();
        applyCondition(bool, request);

        ArticleSearchCondition secondary = request.getSecondary();
        if (secondary != null) {
            bool.must(q -> q.bool(buildCondition(secondary)));
        }
        return bool.build();
    }

    private static BoolQuery buildCondition(ArticleFilterCondition condition) {
        BoolQuery.Builder bool = new BoolQuery.Builder();
        applyCondition(bool, condition);
        return bool.build();
    }

    private static void applyCondition(BoolQuery.Builder bool, ArticleFilterCondition condition) {
        EsSearchScope scope;
        try {
            scope = EsSearchScope.from(condition.getSearchScope());
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage(), e);
        }

        int nearSlop = condition.getNearSlop() == null
                ? SearchExpressionQueryBuilder.DEFAULT_NEAR_SLOP
                : condition.getNearSlop();

        if (StringUtils.hasText(condition.getKeywords())) {
            bool.must(buildQuery(parseKeywords(condition.getKeywords(), "关键词"), nearSlop, scope));
        }
        if (StringUtils.hasText(condition.getExcludeKeywords())) {
            bool.mustNot(buildQuery(parseKeywords(condition.getExcludeKeywords(), "排除关键词"), nearSlop, scope));
        }

        String start = trimToNull(condition.getStartTime());
        String end = trimToNull(condition.getEndTime());
        if (start != null || end != null) {
            bool.filter(f -> f.range(r -> {
                r.field("created_at");
                if (start != null) {
                    r.gte(JsonData.of(start));
                }
                if (end != null) {
                    r.lte(JsonData.of(end));
                }
                return r;
            }));
        }

        addTermsFilter(bool, "source", condition.getSources());
        addTermsFilter(bool, "emotion", condition.getEmotions());
        addTermsFilter(bool, "risk_level", condition.getRiskLevels());
        if (condition.getFilters() != null) {
            for (FieldTermFilter filter : condition.getFilters()) {
                addTermsFilter(bool, filter.field(), filter.values());
            }
        }
    }

    private static SearchExpressionParser.Node parseKeywords(String keywords, String label) {
        try {
            return SearchExpressionParser.parse(keywords);
        } catch (SearchExpressionParser.ParseException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, label + e.getMessage(), e);
        }
    }

    private static Query buildQuery(SearchExpressionParser.Node node, int nearSlop, EsSearchScope scope) {
        try {
            return SearchExpressionQueryBuilder.build(node, nearSlop, scope);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage(), e);
        }
    }

    private static void addTermsFilter(BoolQuery.Builder bool, String field, List<String> values) {
        List<String> normalized = normalizeValues(values);
        if (normalized.isEmpty()) {
            return;
        }
        bool.filter(f -> f.terms(t -> t
                .field(field)
                .terms(tf -> tf.value(normalized.stream().map(FieldValue::of).toList()))
        ));
    }

    private static List<String> normalizeValues(List<String> values) {
        if (values == null || values.isEmpty()) {
            return List.of();
        }
        return values.stream()
                .filter(StringUtils::hasText)
                .map(String::trim)
                .distinct()
                .toList();
    }

    private static String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
