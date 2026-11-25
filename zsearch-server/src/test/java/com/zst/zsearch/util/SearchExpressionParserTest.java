package com.zst.zsearch.util;

import com.zst.zsearch.util.SearchExpressionParser.And;
import com.zst.zsearch.util.SearchExpressionParser.Near;
import com.zst.zsearch.util.SearchExpressionParser.Node;
import com.zst.zsearch.util.SearchExpressionParser.Or;
import com.zst.zsearch.util.SearchExpressionParser.ParseException;
import com.zst.zsearch.util.SearchExpressionParser.Term;
import org.junit.jupiter.api.Test;

import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SearchExpressionParserTest {

    @Test
    void parsesSingleTerm() {
        assertThat(SearchExpressionParser.parse("深圳")).isEqualTo(new Term("深圳"));
    }

    @Test
    void parsesOr() {
        Node node = SearchExpressionParser.parse("深圳|男生");

        assertThat(node).isInstanceOf(Or.class);
        Or or = (Or) node;
        assertThat(or.left()).isEqualTo(new Term("深圳"));
        assertThat(or.right()).isEqualTo(new Term("男生"));
    }

    @Test
    void parsesAnd() {
        Node node = SearchExpressionParser.parse("深圳+男生");

        assertThat(node).isInstanceOf(And.class);
        And and = (And) node;
        assertThat(and.left()).isEqualTo(new Term("深圳"));
        assertThat(and.right()).isEqualTo(new Term("男生"));
    }

    @Test
    void nearHasHigherPrecedenceThanAndAndOr() {
        Node node = SearchExpressionParser.parse("深圳+男生*男性|广州");

        assertThat(node).isInstanceOf(Or.class);
        Or or = (Or) node;
        assertThat(or.left()).isInstanceOf(And.class);
        assertThat(or.right()).isEqualTo(new Term("广州"));

        And and = (And) or.left();
        assertThat(and.left()).isEqualTo(new Term("深圳"));
        assertThat(and.right()).isInstanceOf(Near.class);
    }

    @Test
    void parenthesesOverridePrecedence() {
        Node node = SearchExpressionParser.parse("深圳+(男生|男性)");

        assertThat(node).isInstanceOf(And.class);
        And and = (And) node;
        assertThat(and.left()).isEqualTo(new Term("深圳"));
        assertThat(and.right()).isInstanceOf(Or.class);
    }

    @Test
    void groupsSingleTermTransparently() {
        assertThat(SearchExpressionParser.parse("((深圳))")).isEqualTo(new Term("深圳"));
    }

    @Test
    void nearCollectsAllTerms() {
        Node node = SearchExpressionParser.parse("深圳*男生*男性");

        assertThat(node).isInstanceOf(Near.class);
        Near near = (Near) node;
        assertThat(near.terms()).extracting(Term::value)
                .containsExactly("深圳", "男生", "男性");
    }

    @Test
    void supportsEscapedOperators() {
        Term term = (Term) SearchExpressionParser.parse("A\\|B\\+C\\*D\\(E\\)");

        assertThat(term.value()).isEqualTo("A|B+C*D(E)");
    }

    @Test
    void rejectsBlankExpression() {
        assertThatThrownBy(() -> SearchExpressionParser.parse(null))
                .isInstanceOf(ParseException.class)
                .hasMessageContaining("不能为空");
        assertThatThrownBy(() -> SearchExpressionParser.parse("   "))
                .isInstanceOf(ParseException.class)
                .hasMessageContaining("不能为空");
    }

    @Test
    void rejectsMissingOperands() {
        assertThatThrownBy(() -> SearchExpressionParser.parse("深圳+"))
                .isInstanceOf(ParseException.class)
                .hasMessageContaining("缺少关键词");
        assertThatThrownBy(() -> SearchExpressionParser.parse("|深圳"))
                .isInstanceOf(ParseException.class)
                .hasMessageContaining("前缺少关键词");
        assertThatThrownBy(() -> SearchExpressionParser.parse("深圳**男生"))
                .isInstanceOf(ParseException.class)
                .hasMessageContaining("缺少关键词");
    }

    @Test
    void rejectsAdjacentTermsWithoutOperator() {
        assertThatThrownBy(() -> SearchExpressionParser.parse("深圳 男生"))
                .isInstanceOf(ParseException.class)
                .hasMessageContaining("缺少运算符");
    }

    @Test
    void rejectsGroupAsNearOperand() {
        assertThatThrownBy(() -> SearchExpressionParser.parse("(深圳|广州)*男生"))
                .isInstanceOf(ParseException.class)
                .hasMessageContaining("左侧必须是关键词");
        assertThatThrownBy(() -> SearchExpressionParser.parse("深圳*(男生|男性)"))
                .isInstanceOf(ParseException.class)
                .hasMessageContaining("右侧必须是关键词");
    }

    @Test
    void rejectsBrokenParentheses() {
        assertThatThrownBy(() -> SearchExpressionParser.parse("(深圳+男生"))
                .isInstanceOf(ParseException.class)
                .hasMessageContaining("缺少右括号");
        assertThatThrownBy(() -> SearchExpressionParser.parse("深圳+男生)"))
                .isInstanceOf(ParseException.class)
                .hasMessageContaining("多余的右括号");
        assertThatThrownBy(() -> SearchExpressionParser.parse("()"))
                .isInstanceOf(ParseException.class)
                .hasMessageContaining("括号内不能为空");
    }

    @Test
    void rejectsDanglingEscape() {
        assertThatThrownBy(() -> SearchExpressionParser.parse("深圳\\"))
                .isInstanceOf(ParseException.class)
                .hasMessageContaining("转义符");
    }

    @Test
    void rejectsTooManyTerms() {
        String expression = IntStream.rangeClosed(1, SearchExpressionParser.MAX_TERMS + 1)
                .mapToObj(i -> "k" + i)
                .collect(Collectors.joining("+"));

        assertThatThrownBy(() -> SearchExpressionParser.parse(expression))
                .isInstanceOf(ParseException.class)
                .hasMessageContaining("关键词数量");
    }

    @Test
    void rejectsTooDeepNesting() {
        String expression = "(".repeat(SearchExpressionParser.MAX_DEPTH + 1)
                + "深圳"
                + ")".repeat(SearchExpressionParser.MAX_DEPTH + 1);

        assertThatThrownBy(() -> SearchExpressionParser.parse(expression))
                .isInstanceOf(ParseException.class)
                .hasMessageContaining("嵌套");
    }

    @Test
    void rejectsTooLongExpression() {
        String expression = "a".repeat(SearchExpressionParser.MAX_EXPRESSION_LENGTH + 1);

        assertThatThrownBy(() -> SearchExpressionParser.parse(expression))
                .isInstanceOf(ParseException.class)
                .hasMessageContaining("不能超过");
    }
}
