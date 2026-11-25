package com.zst.zsearch.util;

import java.util.ArrayList;
import java.util.List;

public final class SearchExpressionParser {

    // 方案关键词最长 2000 字；即时搜索的 500 字上限由请求 DTO 校验
    public static final int MAX_EXPRESSION_LENGTH = 2000;
    public static final int MAX_TERMS = 128;
    public static final int MAX_DEPTH = 10;

    private SearchExpressionParser() {
    }

    public static Node parse(String expression) {
        if (expression == null || expression.isBlank()) {
            throw new ParseException(1, "检索表达式不能为空");
        }
        if (expression.length() > MAX_EXPRESSION_LENGTH) {
            throw new ParseException(MAX_EXPRESSION_LENGTH + 1,
                    "检索表达式不能超过 " + MAX_EXPRESSION_LENGTH + " 个字符");
        }
        return new Parser(expression).parse();
    }

    public sealed interface Node permits Term, And, Or, Near {
    }

    public record Term(String value) implements Node {
    }

    public record And(Node left, Node right) implements Node {
    }

    public record Or(Node left, Node right) implements Node {
    }

    public record Near(List<Term> terms) implements Node {
        public Near {
            terms = List.copyOf(terms);
        }
    }

    public static class ParseException extends IllegalArgumentException {
        private static final long serialVersionUID = 1L;

        private final int position;

        public ParseException(int position, String message) {
            super("表达式解析失败（第 " + position + " 个字符）: " + message);
            this.position = position;
        }

        public int getPosition() {
            return position;
        }
    }

    private enum TokenType {
        TERM, OR, AND, NEAR, LPAREN, RPAREN, EOF
    }

    private record Token(TokenType type, String text, int position) {
    }

    private static final class Lexer {
        private final String expression;
        private int index;

        private Lexer(String expression) {
            this.expression = expression;
        }

        private Token next() {
            while (index < expression.length() && Character.isWhitespace(expression.charAt(index))) {
                index++;
            }
            if (index >= expression.length()) {
                return new Token(TokenType.EOF, "", expression.length() + 1);
            }

            int start = index;
            char current = expression.charAt(index);
            switch (current) {
                case '|':
                    index++;
                    return new Token(TokenType.OR, "|", start + 1);
                case '+':
                    index++;
                    return new Token(TokenType.AND, "+", start + 1);
                case '*':
                    index++;
                    return new Token(TokenType.NEAR, "*", start + 1);
                case '(':
                    index++;
                    return new Token(TokenType.LPAREN, "(", start + 1);
                case ')':
                    index++;
                    return new Token(TokenType.RPAREN, ")", start + 1);
                default:
                    return readTerm();
            }
        }

        private Token readTerm() {
            int start = index;
            StringBuilder value = new StringBuilder();
            while (index < expression.length()) {
                char current = expression.charAt(index);
                if (current == '\\') {
                    int slashPosition = index + 1;
                    index++;
                    if (index >= expression.length()) {
                        throw new ParseException(slashPosition, "转义符 \\ 后缺少字符");
                    }
                    value.append(expression.charAt(index));
                    index++;
                } else if (current == '|' || current == '+' || current == '*'
                        || current == '(' || current == ')' || Character.isWhitespace(current)) {
                    break;
                } else {
                    value.append(current);
                    index++;
                }
            }

            String term = value.toString();
            if (term.isBlank()) {
                throw new ParseException(start + 1, "关键词不能为空");
            }
            return new Token(TokenType.TERM, term, start + 1);
        }
    }

    private static final class Parser {
        private final Lexer lexer;
        private Token current;
        private int termCount;
        private int depth;

        private Parser(String expression) {
            this.lexer = new Lexer(expression);
            this.current = lexer.next();
        }

        private Node parse() {
            Node node = parseOr();
            if (current.type() != TokenType.EOF) {
                if (current.type() == TokenType.RPAREN) {
                    throw error(current.position(), "多余的右括号；如需搜索 ) 本身，请使用 \\) 转义");
                }
                throw error(current.position(), "缺少运算符：关键词之间必须使用 |、+ 或 * 连接");
            }
            return node;
        }

        private Node parseOr() {
            Node left = parseAnd();
            while (current.type() == TokenType.OR) {
                advance();
                left = new Or(left, parseAnd());
            }
            return left;
        }

        private Node parseAnd() {
            Node left = parseNear();
            while (current.type() == TokenType.AND) {
                advance();
                left = new And(left, parseNear());
            }
            return left;
        }

        private Node parseNear() {
            Node first = parsePrimary();
            if (current.type() != TokenType.NEAR) {
                return first;
            }
            if (!(first instanceof Term firstTerm)) {
                throw error(current.position(),
                        "邻近运算符 * 左侧必须是关键词，不支持对括号表达式做邻近查询");
            }

            List<Term> terms = new ArrayList<>();
            terms.add(firstTerm);
            while (current.type() == TokenType.NEAR) {
                Token operator = current;
                advance();
                if (current.type() != TokenType.TERM && current.type() != TokenType.LPAREN) {
                    throw error(operator.position(), "邻近运算符 * 右侧缺少关键词");
                }
                Node right = parsePrimary();
                if (!(right instanceof Term term)) {
                    throw error(operator.position(),
                            "邻近运算符 * 右侧必须是关键词，不支持对括号表达式做邻近查询");
                }
                terms.add(term);
            }
            return new Near(terms);
        }

        private Node parsePrimary() {
            Token token = current;
            switch (token.type()) {
                case TERM:
                    advance();
                    termCount++;
                    if (termCount > MAX_TERMS) {
                        throw error(token.position(), "关键词数量不能超过 " + MAX_TERMS + " 个");
                    }
                    return new Term(token.text());
                case LPAREN:
                    advance();
                    depth++;
                    if (depth > MAX_DEPTH) {
                        throw error(token.position(), "括号嵌套层级不能超过 " + MAX_DEPTH + " 层");
                    }
                    if (current.type() == TokenType.RPAREN) {
                        throw error(current.position(), "括号内不能为空");
                    }
                    Node node = parseOr();
                    if (current.type() != TokenType.RPAREN) {
                        throw error(current.position(), "缺少右括号");
                    }
                    advance();
                    depth--;
                    return node;
                case EOF:
                    throw error(token.position(), "表达式不完整，缺少关键词");
                case RPAREN:
                    throw error(token.position(), "多余的右括号；如需搜索 ) 本身，请使用 \\) 转义");
                default:
                    throw error(token.position(),
                            "运算符 " + token.text() + " 前缺少关键词；如需搜索该字符本身，请使用 \\" + token.text() + " 转义");
            }
        }

        private void advance() {
            current = lexer.next();
        }

        private ParseException error(int position, String message) {
            return new ParseException(position, message);
        }
    }
}
