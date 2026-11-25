package com.zst.zsearch.common.enums;

import lombok.Getter;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

@Getter
public enum ArticleSortField {

    CREATED_AT("createdAt", "created_at", true),
    CRAWLER_TIME("crawlerTime", "crawler_time", true),
    SCORE("_score", "_score", true),
    REPOST_COUNT("repostCount", "repost_count", false),
    COMMENT_COUNT("commentCount", "comment_count", false),
    LIKE_COUNT("likeCount", "like_count", false),
    FOLLOWER_COUNT("followerCount", "follower_count", false),
    COLLECT_COUNT("collectCount", "collect_count", false),
    INTERACTION_COUNT("interactionCount", "interaction_count", false);

    private static final Map<String, ArticleSortField> LOOKUP = new HashMap<>();

    static {
        for (ArticleSortField field : values()) {
            LOOKUP.put(normalize(field.value), field);
            LOOKUP.put(normalize(field.esField), field);
        }
    }

    private final String value;
    private final String esField;
    private final boolean ascAllowed;

    ArticleSortField(String value, String esField, boolean ascAllowed) {
        this.value = value;
        this.esField = esField;
        this.ascAllowed = ascAllowed;
    }

    public static ArticleSortField from(String value) {
        if (value == null || value.isBlank()) {
            return CREATED_AT;
        }
        ArticleSortField field = LOOKUP.get(normalize(value.trim()));
        if (field == null) {
            throw new IllegalArgumentException("不支持的排序字段: " + value);
        }
        return field;
    }

    public boolean supports(SortOrder order) {
        return order != SortOrder.ASC || ascAllowed;
    }

    private static String normalize(String value) {
        return value.replace("_", "").toLowerCase(Locale.ROOT);
    }
}
