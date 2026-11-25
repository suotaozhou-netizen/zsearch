package com.zst.zsearch.es.query;

import lombok.Getter;

import java.util.List;
import java.util.Locale;

@Getter
public enum EsSearchScope {

    FULL_TEXT(
            List.of("title^2.0", "text^1.5", "ocr^1.0", "asr^1.0",
                    "title.ngram^0.5", "text.ngram^0.3"),
            "text"
    ),
    TITLE(
            List.of("title^2.0", "title.ngram^1.0"),
            "title"
    ),
    NAME(
            List.of("name"),
            null
    ),
    SOURCE(
            List.of("source"),
            null
    );

    private final List<String> queryFields;
    private final String nearField;

    EsSearchScope(List<String> queryFields, String nearField) {
        this.queryFields = queryFields;
        this.nearField = nearField;
    }

    public static EsSearchScope from(String value) {
        if (value == null || value.isBlank()) {
            return FULL_TEXT;
        }
        try {
            return valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("不支持的搜索方式: " + value);
        }
    }
}
