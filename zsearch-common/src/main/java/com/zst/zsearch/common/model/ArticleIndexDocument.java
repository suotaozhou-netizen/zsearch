package com.zst.zsearch.common.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * 写入 Elasticsearch 的文档结构。文档 _id 使用 articleId。
 * ES 客户端使用 SNAKE_CASE 命名策略；以下字段在现有 mapping 中为显式驼峰，必须保持。
 */
@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ArticleIndexDocument {

    private String id;
    private String title;
    private String text;
    private String summary;
    private String name;
    private String source;
    private String url;
    @JsonProperty("homeUrl")
    private String homeUrl;
    private String avatar;
    @JsonProperty("siteId")
    private String siteId;
    @JsonProperty("userId")
    private String userId;
    @JsonProperty("imageList")
    private List<String> imageList;
    @JsonProperty("videoList")
    private List<String> videoList;
    private String createdAt;
    private String crawlerTime;
    private String emotion;
    private String riskLevel;
    private String ocr;
    private String asr;
    private Integer textLength;
    private Long repostCount;
    private Long commentCount;
    private Long likeCount;
    private Long followerCount;
    private Long collectCount;
    private Long interactionCount;

    private Long rawVersion;
    private Integer pipelineVersion;
    private String contentHash;
    private String indexedAt;
}
