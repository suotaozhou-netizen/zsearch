package com.zst.zsearch.common.id;

import com.alibaba.fastjson2.JSON;
import com.zst.zsearch.common.model.RawArticle;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 内容指纹：对“内容相关字段”的规范化 JSON 做 SHA-256。
 * 刻意排除 crawlerTime 与 rawVersion，使“仅重新采集、内容未变”的重复消息可被去重。
 * 注意：修改参与哈希的字段或规范化方式，等同于修改去重语义，会导致存量数据被重写一次。
 */
public final class ContentFingerprint {

    private static final HexFormat HEX = HexFormat.of();

    private ContentFingerprint() {
    }

    public static String of(RawArticle article) {
        Map<String, Object> content = new LinkedHashMap<>();
        content.put("url", text(article.getUrl()));
        content.put("title", text(article.getTitle()));
        content.put("text", text(article.getText()));
        content.put("summary", text(article.getSummary()));
        content.put("name", text(article.getName()));
        content.put("source", text(article.getSource()));
        content.put("homeUrl", text(article.getHomeUrl()));
        content.put("avatar", text(article.getAvatar()));
        content.put("createdAt", text(article.getCreatedAt()));
        content.put("emotion", text(article.getEmotion()));
        content.put("riskLevel", text(article.getRiskLevel()));
        content.put("ocr", text(article.getOcr()));
        content.put("asr", text(article.getAsr()));
        content.put("repostCount", count(article.getRepostCount()));
        content.put("commentCount", count(article.getCommentCount()));
        content.put("likeCount", count(article.getLikeCount()));
        content.put("followerCount", count(article.getFollowerCount()));
        content.put("collectCount", count(article.getCollectCount()));
        content.put("siteId", text(article.getSiteId()));
        content.put("userId", text(article.getUserId()));
        content.put("imageList", list(article.getImageList()));
        content.put("videoList", list(article.getVideoList()));

        return sha256(JSON.toJSONString(content));
    }

    private static String text(String value) {
        return value == null ? "" : value.trim();
    }

    private static long count(Long value) {
        return value == null ? -1L : value;
    }

    private static List<String> list(List<String> value) {
        return value == null ? List.of() : value;
    }

    private static String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HEX.formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 算法不可用", e);
        }
    }
}
