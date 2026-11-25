package com.zst.zsearch.streaming.validate;

import com.zst.zsearch.common.contract.SchemaVersion;
import com.zst.zsearch.common.enums.SiteId;
import com.zst.zsearch.common.id.ArticleIdGenerator;
import com.zst.zsearch.common.model.IngestEnvelope;
import com.zst.zsearch.common.model.RawArticle;

import java.util.regex.Pattern;

/**
 * 消息契约校验（§4.3）。articleId 与 url 重算结果不一致视为契约漂移，直接拒绝。
 */
public final class ArticleValidator {

    public static final String SCHEMA_ERROR = "SCHEMA_ERROR";
    public static final String ID_INVALID = "ID_INVALID";
    public static final String ID_MISMATCH = "ID_MISMATCH";
    public static final String VERSION_INVALID = "VERSION_INVALID";
    public static final String ARTICLE_EMPTY = "ARTICLE_EMPTY";
    public static final String URL_EMPTY = "URL_EMPTY";
    public static final String CONTENT_EMPTY = "CONTENT_EMPTY";
    public static final String SITE_INVALID = "SITE_INVALID";

    private static final Pattern ARTICLE_ID_PATTERN = Pattern.compile("^[0-9a-f]{32}$");

    private ArticleValidator() {
    }

    public record Error(String type, String message) {
    }

    public static Error validate(IngestEnvelope envelope) {
        if (envelope == null) {
            return new Error(SCHEMA_ERROR, "消息体为空");
        }
        if (envelope.getSchemaVersion() == null || envelope.getSchemaVersion() != SchemaVersion.CURRENT) {
            return new Error(SCHEMA_ERROR, "不支持的 schemaVersion: " + envelope.getSchemaVersion());
        }
        if (envelope.getIdVersion() == null || envelope.getIdVersion() != SchemaVersion.ARTICLE_ID_V1) {
            return new Error(SCHEMA_ERROR, "不支持的 idVersion: " + envelope.getIdVersion());
        }
        if (envelope.getArticleId() == null || !ARTICLE_ID_PATTERN.matcher(envelope.getArticleId()).matches()) {
            return new Error(ID_INVALID, "articleId 必须为 32 位小写十六进制: " + envelope.getArticleId());
        }
        if (envelope.getRawVersion() == null || envelope.getRawVersion() <= 0) {
            return new Error(VERSION_INVALID, "rawVersion 必须为正数: " + envelope.getRawVersion());
        }

        RawArticle article = envelope.getArticle();
        if (article == null) {
            return new Error(ARTICLE_EMPTY, "article 不能为空");
        }
        if (!hasText(article.getUrl())) {
            return new Error(URL_EMPTY, "article.url 不能为空");
        }
        if (!hasText(article.getTitle()) && !hasText(article.getText())) {
            return new Error(CONTENT_EMPTY, "article.title 与 article.text 不能同时为空");
        }
        Error siteError = validateSiteId(article.getSiteId());
        if (siteError != null) {
            return siteError;
        }

        String expectedId;
        try {
            expectedId = ArticleIdGenerator.fromUrl(article.getUrl());
        } catch (IllegalArgumentException e) {
            return new Error(URL_EMPTY, e.getMessage());
        }
        if (!expectedId.equals(envelope.getArticleId())) {
            return new Error(ID_MISMATCH,
                    "articleId 与 url 不匹配: expected=" + expectedId + ", actual=" + envelope.getArticleId());
        }
        return null;
    }

    private static Error validateSiteId(String siteId) {
        if (!hasText(siteId)) {
            return new Error(SITE_INVALID, "siteId 不能为空");
        }
        try {
            SiteId.fromCode(Integer.parseInt(siteId.trim()));
            return null;
        } catch (IllegalArgumentException e) {
            return new Error(SITE_INVALID, "不支持的站点id: " + siteId);
        }
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
