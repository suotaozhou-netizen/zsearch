package com.zst.zsearch.es.query;

import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import com.zst.zsearch.util.SearchExpressionParser;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SearchExpressionQueryBuilderTest {

    @Test
    void buildsMustForAnd() {
        String json = build("深圳+男生", 5);

        assertThat(json).contains("\"must\"");
    }

    @Test
    void buildsShouldForOr() {
        String json = build("深圳|男生", 5);

        assertThat(json).contains("\"should\"");
        assertThat(json).contains("minimum_should_match");
    }

    @Test
    void buildsNestedBoolForMixedExpression() {
        String json = build("深圳+(男生|男性)", 5);

        assertThat(json).contains("\"must\"");
        assertThat(json).contains("\"should\"");
    }

    @Test
    void buildsIntervalsForNear() {
        String json = build("深圳*男生", 5);

        assertThat(json).contains("\"intervals\"");
        assertThat(json).contains("\"text\"");
        assertThat(json).contains("\"all_of\"");
        assertThat(json).contains("\"max_gaps\"");
        assertThat(json).contains("\"ordered\"");
    }

    @Test
    void searchesTextOcrAndAsrFields() {
        String json = build("深圳", 5);

        assertThat(json).contains("\"text^1.5\"");
        assertThat(json).contains("\"ocr^1.0\"");
        assertThat(json).contains("\"asr^1.0\"");
    }

    @Test
    void usesSevenAsDefaultNearSlop() {
        assertThat(SearchExpressionQueryBuilder.DEFAULT_NEAR_SLOP).isEqualTo(7);
    }

    @Test
    void buildsTitleScopeWithTitleFields() {
        Query query = SearchExpressionQueryBuilder.build(
                SearchExpressionParser.parse("深圳"), 7, EsSearchScope.TITLE);

        assertThat(query.toString())
                .contains("title^2.0")
                .doesNotContain("text^1.5");
    }

    @Test
    void rejectsNearForKeywordScope() {
        assertThatThrownBy(() -> SearchExpressionQueryBuilder.build(
                SearchExpressionParser.parse("深圳*男生"), 7, EsSearchScope.SOURCE))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("邻近");
    }

    @Test
    void acceptsZeroNearSlop() {
        String json = build("深圳*男生", 0);

        assertThat(json).contains("\"max_gaps\"");
    }

    @Test
    void rejectsInvalidNearSlop() {
        assertThatThrownBy(() -> build("深圳*男生", SearchExpressionQueryBuilder.MAX_NEAR_SLOP + 1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("nearSlop");
    }

    private String build(String expression, int nearSlop) {
        Query query = SearchExpressionQueryBuilder.build(
                SearchExpressionParser.parse(expression),
                nearSlop
        );
        return query.toString();
    }
}
