package com.zst.zsearch.common.id;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * articleId 算法 v1：md5(url.trim())。
 * 该算法是持久契约，修改会导致历史文档全部“换 id”，必须走全量重建索引。
 */
public final class ArticleIdGenerator {

    private static final HexFormat HEX = HexFormat.of();

    private ArticleIdGenerator() {
    }

    public static String fromUrl(String url) {
        if (url == null || url.isBlank()) {
            throw new IllegalArgumentException("文章 URL 不能为空");
        }
        try {
            MessageDigest md5 = MessageDigest.getInstance("MD5");
            byte[] digest = md5.digest(url.trim().getBytes(StandardCharsets.UTF_8));
            return HEX.formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("MD5 算法不可用", e);
        }
    }
}
