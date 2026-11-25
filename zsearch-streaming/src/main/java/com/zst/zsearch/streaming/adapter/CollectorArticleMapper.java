package com.zst.zsearch.streaming.adapter;

import com.alibaba.fastjson2.JSONObject;
import com.zst.zsearch.common.enums.SiteId;
import com.zst.zsearch.common.model.RawArticle;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * 将采集端（MediaCrawler）原生事件 payload 映射为流水线 RawArticle。
 * 只支持内容类文章；评论、探针在适配层直接跳过。
 */
final class CollectorArticleMapper {

    static final String UNSUPPORTED_PLATFORM = "UNSUPPORTED_PLATFORM";
    static final String PAYLOAD_INVALID = "PAYLOAD_INVALID";

    private static final ZoneId ZONE = ZoneId.of("Asia/Shanghai");
    private static final long EPOCH_MILLIS_THRESHOLD = 100_000_000_000L;

    private CollectorArticleMapper() {
    }

    static Mapped map(String platform, JSONObject payload, Long crawledAt, long kafkaTimestamp) {
        if (payload == null) {
            throw new MappingException(PAYLOAD_INVALID, "payload 不能为空");
        }
        String normalized = platform == null ? "" : platform.trim().toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "bili", "bilibili" -> mapBili(payload, crawledAt, kafkaTimestamp);
            case "dy", "douyin" -> mapDouyin(payload, crawledAt, kafkaTimestamp);
            default -> throw new MappingException(UNSUPPORTED_PLATFORM, "暂不支持的平台: " + platform);
        };
    }

    private static Mapped mapBili(JSONObject payload, Long crawledAt, long kafkaTimestamp) {
        String url = text(payload, "video_url");
        if (!hasText(url)) {
            throw new MappingException(PAYLOAD_INVALID, "bili payload 缺少 video_url");
        }
        String title = text(payload, "title");
        String content = text(payload, "desc");
        if (!hasText(title) && !hasText(content)) {
            throw new MappingException(PAYLOAD_INVALID, "bili payload 的 title 与 desc 不能同时为空");
        }

        long rawVersion = resolveRawVersion(payload, crawledAt, kafkaTimestamp);
        String crawled = resolveCrawledAt(crawledAt, kafkaTimestamp);

        RawArticle article = new RawArticle();
        article.setUrl(url.trim());
        article.setTitle(title);
        article.setText(content);
        article.setName(text(payload, "nickname"));
        article.setSource("bilibili");
        article.setSiteId(String.valueOf(SiteId.BILIBILI.getCode()));
        article.setUserId(text(payload, "creator_hash"));
        article.setCreatedAt(formatTimestamp(payload.get("create_time")));
        article.setCrawlerTime(crawled);
        article.setLikeCount(toLong(payload.get("liked_count")));
        article.setCommentCount(toLong(payload.get("video_comment")));
        article.setRepostCount(toLong(payload.get("video_share_count")));
        article.setCollectCount(toLong(payload.get("video_favorite_count")));
        article.setRawVersion(rawVersion);

        applyCover(article, text(payload, "video_cover_url"));
        return new Mapped(article, rawVersion, crawled);
    }

    /**
     * 抖音（MediaCrawler dy）字段映射：aweme_url→url、title→title、desc→text、
     * nickname→name、creator_hash→userId、create_time→createdAt、cover_url→imageList，
     * 互动计数 like/comment/share/collected 同名映射。
     */
    private static Mapped mapDouyin(JSONObject payload, Long crawledAt, long kafkaTimestamp) {
        String url = text(payload, "aweme_url");
        if (!hasText(url)) {
            throw new MappingException(PAYLOAD_INVALID, "dy payload 缺少 aweme_url");
        }
        String title = text(payload, "title");
        String content = text(payload, "desc");
        if (!hasText(title) && !hasText(content)) {
            throw new MappingException(PAYLOAD_INVALID, "dy payload 的 title 与 desc 不能同时为空");
        }

        long rawVersion = resolveRawVersion(payload, crawledAt, kafkaTimestamp);
        String crawled = resolveCrawledAt(crawledAt, kafkaTimestamp);

        RawArticle article = new RawArticle();
        article.setUrl(url.trim());
        article.setTitle(title);
        article.setText(content);
        article.setName(text(payload, "nickname"));
        article.setSource("douyin");
        article.setSiteId(String.valueOf(SiteId.DOUYIN.getCode()));
        article.setUserId(text(payload, "creator_hash"));
        article.setCreatedAt(formatTimestamp(payload.get("create_time")));
        article.setCrawlerTime(crawled);
        article.setLikeCount(toLong(payload.get("liked_count")));
        article.setCommentCount(toLong(payload.get("comment_count")));
        article.setRepostCount(toLong(payload.get("share_count")));
        article.setCollectCount(toLong(payload.get("collected_count")));
        article.setRawVersion(rawVersion);

        applyCover(article, text(payload, "cover_url"));
        return new Mapped(article, rawVersion, crawled);
    }

    private static void applyCover(RawArticle article, String cover) {
        if (!hasText(cover)) {
            return;
        }
        List<String> images = new ArrayList<>();
        images.add(cover.trim());
        article.setImageList(images);
    }

    private static long resolveRawVersion(JSONObject payload, Long crawledAt, long kafkaTimestamp) {
        return firstPositive(
                nullToZero(toLong(payload.get("last_modify_ts"))),
                crawledAt == null ? 0L : crawledAt,
                kafkaTimestamp);
    }

    private static String resolveCrawledAt(Long crawledAt, long kafkaTimestamp) {
        return isoFromMillis(crawledAt != null && crawledAt > 0 ? crawledAt : kafkaTimestamp);
    }

    private static long nullToZero(Long value) {
        return value == null ? 0L : value;
    }

    private static long firstPositive(long... values) {
        for (long value : values) {
            if (value > 0) {
                return value;
            }
        }
        throw new MappingException(PAYLOAD_INVALID, "无法确定 rawVersion");
    }

    private static String formatTimestamp(Object value) {
        Long raw = toLong(value);
        if (raw == null || raw <= 0) {
            return null;
        }
        long millis = raw < EPOCH_MILLIS_THRESHOLD ? raw * 1000 : raw;
        return isoFromMillis(millis);
    }

    private static String isoFromMillis(long millis) {
        return OffsetDateTime.ofInstant(Instant.ofEpochMilli(millis), ZONE).toString();
    }

    private static String text(JSONObject payload, String key) {
        Object value = payload.get(key);
        return value == null ? null : value.toString();
    }

    static Long toLong(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        String text = value.toString().trim();
        if (text.isEmpty()) {
            return null;
        }
        try {
            return Long.parseLong(text);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    record Mapped(RawArticle article, long rawVersion, String crawledAt) {
    }

    static final class MappingException extends RuntimeException {

        private final String type;

        MappingException(String type, String message) {
            super(message);
            this.type = type;
        }

        String type() {
            return type;
        }
    }
}
