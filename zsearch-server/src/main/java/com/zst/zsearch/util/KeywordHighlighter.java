package com.zst.zsearch.util;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class KeywordHighlighter {

    private KeywordHighlighter() {
    }

    public static List<String> parseTerms(String expression) {
        if (expression == null || expression.isBlank()) {
            return List.of();
        }
        Set<String> terms = new LinkedHashSet<>();
        collectTerms(SearchExpressionParser.parse(expression.trim()), terms);
        return List.copyOf(terms);
    }

    public static List<String> parseTerms(List<String> expressions) {
        if (expressions == null || expressions.isEmpty()) {
            return List.of();
        }
        Set<String> terms = new LinkedHashSet<>();
        for (String expression : expressions) {
            if (expression == null || expression.isBlank()) {
                continue;
            }
            collectTerms(SearchExpressionParser.parse(expression.trim()), terms);
        }
        return List.copyOf(terms);
    }

    public static Map<String, Integer> countHits(List<String> terms, String... contents) {
        Map<String, Integer> result = new LinkedHashMap<>();
        if (terms == null || terms.isEmpty() || contents == null) {
            return result;
        }
        for (String term : terms) {
            if (term == null || term.isEmpty()) {
                continue;
            }
            int count = 0;
            for (String content : contents) {
                if (content == null || content.isEmpty()) {
                    continue;
                }
                count += countOccurrences(content, term);
            }
            if (count > 0) {
                result.put(term, count);
            }
        }
        return result;
    }

    private static void collectTerms(SearchExpressionParser.Node node, Set<String> terms) {
        if (node instanceof SearchExpressionParser.Term term) {
            terms.add(term.value());
        } else if (node instanceof SearchExpressionParser.And and) {
            collectTerms(and.left(), terms);
            collectTerms(and.right(), terms);
        } else if (node instanceof SearchExpressionParser.Or or) {
            collectTerms(or.left(), terms);
            collectTerms(or.right(), terms);
        } else if (node instanceof SearchExpressionParser.Near near) {
            for (SearchExpressionParser.Term term : near.terms()) {
                terms.add(term.value());
            }
        }
    }

    private static int countOccurrences(String content, String term) {
        int count = 0;
        int from = 0;
        while (from <= content.length() - term.length()) {
            if (content.regionMatches(true, from, term, 0, term.length())) {
                count++;
                from += term.length();
            } else {
                from++;
            }
        }
        return count;
    }
}
