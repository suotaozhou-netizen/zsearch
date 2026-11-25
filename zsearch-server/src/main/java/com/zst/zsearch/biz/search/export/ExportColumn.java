package com.zst.zsearch.biz.search.export;

import com.zst.zsearch.es.dto.Article;
import lombok.Getter;
import org.springframework.http.HttpStatus;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Getter
public enum ExportColumn {

    TITLE("title", "标题", "title", 40, Article::getTitle),
    TEXT("text", "正文", "text", 60, Article::getText),
    NAME("name", "用户名", "name", 20, Article::getName),
    SOURCE("source", "信息来源", "source", 16, Article::getSource),
    URL("url", "原文链接", "url", 50, Article::getUrl),
    IMAGE_LIST("imageList", "原文图片", "imageList", 50, ExportColumn::imageLinks),
    VIDEO_LIST("videoList", "原文视频", "videoList", 50, ExportColumn::videoLinks),
    CREATED_AT("createdAt", "发布时间", "created_at", 22, Article::getCreatedAt),
    EMOTION("emotion", "情感类型", "emotion", 12, Article::getEmotion),
    RISK_LEVEL("riskLevel", "风险等级", "risk_level", 12, Article::getRiskLevel),
    OCR("ocr", "OCR", "ocr", 50, Article::getOcr),
    ASR("asr", "ASR", "asr", 50, Article::getAsr),
    REPOST_COUNT("repostCount", "转发数", "repost_count", 12, Article::getRepostCount),
    COMMENT_COUNT("commentCount", "评论数", "comment_count", 12, Article::getCommentCount),
    LIKE_COUNT("likeCount", "点赞数", "like_count", 12, Article::getLikeCount),
    FOLLOWER_COUNT("followerCount", "粉丝数", "follower_count", 12, Article::getFollowerCount),
    COLLECT_COUNT("collectCount", "收藏数", "collect_count", 12, Article::getCollectCount);

    private static final Map<String, ExportColumn> LOOKUP = new HashMap<>();

    static {
        for (ExportColumn column : values()) {
            LOOKUP.put(normalize(column.key), column);
            LOOKUP.put(normalize(column.esField), column);
        }
    }

    /**
     * 前端使用的字段标识
     */
    private final String key;

    /**
     * Excel 表头
     */
    private final String header;

    /**
     * ES 原始字段名（用于 _source 过滤）
     */
    private final String esField;

    /**
     * 列宽（字符数）
     */
    private final int width;

    private final Function<Article, Object> extractor;

    ExportColumn(String key, String header, String esField, int width, Function<Article, Object> extractor) {
        this.key = key;
        this.header = header;
        this.esField = esField;
        this.width = width;
        this.extractor = extractor;
    }

    public Object extract(Article article) {
        return extractor.apply(article);
    }

    public static List<ExportColumn> resolve(List<String> keys) {
        if (keys == null || keys.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "导出字段不能为空");
        }
        EnumSet<ExportColumn> selected = EnumSet.noneOf(ExportColumn.class);
        for (String key : keys) {
            if (!StringUtils.hasText(key)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "导出字段不能包含空值");
            }
            ExportColumn column = LOOKUP.get(normalize(key));
            if (column == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "不支持的导出字段: " + key + "，可选字段: " + supportedKeys());
            }
            selected.add(column);
        }
        if (selected.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "导出字段不能为空");
        }
        return List.copyOf(selected);
    }

    public static List<String> esFields(List<ExportColumn> columns) {
        return columns.stream()
                .map(ExportColumn::getEsField)
                .distinct()
                .toList();
    }

    private static String supportedKeys() {
        return Stream.of(values())
                .map(ExportColumn::getKey)
                .collect(Collectors.joining(", "));
    }

    private static String normalize(String value) {
        return value.trim().toLowerCase(Locale.ROOT);
    }

    private static String imageLinks(Article article) {
        return joinLinks(article.getImageList());
    }

    private static String videoLinks(Article article) {
        return joinLinks(article.getVideoList());
    }

    private static String joinLinks(List<String> links) {
        if (links == null || links.isEmpty()) {
            return null;
        }
        String joined = links.stream()
                .filter(StringUtils::hasText)
                .map(String::trim)
                .collect(Collectors.joining("\n"));
        return joined.isEmpty() ? null : joined;
    }
}