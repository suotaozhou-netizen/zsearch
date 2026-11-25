package com.zst.zsearch.es.query;

import co.elastic.clients.elasticsearch._types.query_dsl.Intervals;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch._types.query_dsl.TextQueryType;
import com.zst.zsearch.util.SearchExpressionParser.And;
import com.zst.zsearch.util.SearchExpressionParser.Near;
import com.zst.zsearch.util.SearchExpressionParser.Node;
import com.zst.zsearch.util.SearchExpressionParser.Or;
import com.zst.zsearch.util.SearchExpressionParser.Term;

import java.util.List;

public final class SearchExpressionQueryBuilder {

    public static final int DEFAULT_NEAR_SLOP = 7;
    public static final int MAX_NEAR_SLOP = 20;

    private SearchExpressionQueryBuilder() {
    }

    public static Query build(Node node, int nearSlop) {
        return build(node, nearSlop, EsSearchScope.FULL_TEXT);
    }

    public static Query build(Node node, int nearSlop, EsSearchScope scope) {
        if (node == null) {
            throw new IllegalArgumentException("表达式节点不能为空");
        }
        if (scope == null) {
            throw new IllegalArgumentException("搜索方式不能为空");
        }
        if (nearSlop < 0 || nearSlop > MAX_NEAR_SLOP) {
            throw new IllegalArgumentException("nearSlop 必须在 0 到 " + MAX_NEAR_SLOP + " 之间");
        }
        return buildNode(node, nearSlop, scope);
    }

    private static Query buildNode(Node node, int nearSlop, EsSearchScope scope) {
        if (node instanceof Term term) {
            return termQuery(term.value(), scope);
        }
        if (node instanceof And and) {
            return boolMust(buildNode(and.left(), nearSlop, scope),
                    buildNode(and.right(), nearSlop, scope));
        }
        if (node instanceof Or or) {
            return boolShould(buildNode(or.left(), nearSlop, scope),
                    buildNode(or.right(), nearSlop, scope));
        }
        if (node instanceof Near near) {
            return nearQuery(near, nearSlop, scope);
        }
        throw new IllegalArgumentException("不支持的表达式节点: " + node.getClass().getName());
    }

    private static Query termQuery(String value, EsSearchScope scope) {
        return Query.of(query -> query.multiMatch(multiMatch -> multiMatch
                .query(value)
                .fields(scope.getQueryFields())
                .type(TextQueryType.BestFields)
                .minimumShouldMatch("100%")
        ));
    }

    private static Query boolMust(Query left, Query right) {
        return Query.of(query -> query.bool(bool -> bool.must(left, right)));
    }

    private static Query boolShould(Query left, Query right) {
        return Query.of(query -> query.bool(bool -> bool
                .should(left, right)
                .minimumShouldMatch("1")
        ));
    }

    private static Query nearQuery(Near near, int maxGaps, EsSearchScope scope) {
        if (scope.getNearField() == null) {
            throw new IllegalArgumentException("当前搜索方式不支持邻近检索 *");
        }
        List<Intervals> intervals = near.terms().stream()
                .map(term -> Intervals.of(interval -> interval.match(match -> match.query(term.value()))))
                .toList();
        // 站点语义是“中间不超过 7 个字符”，ES intervals 按 token 间隔近似
        return Query.of(query -> query.intervals(intervalsQuery -> intervalsQuery
                .field(scope.getNearField())
                .allOf(allOf -> allOf
                        .ordered(false)
                        .maxGaps(maxGaps)
                        .intervals(intervals)
                )
        ));
    }
}
