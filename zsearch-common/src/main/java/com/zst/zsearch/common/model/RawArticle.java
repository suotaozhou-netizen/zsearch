package com.zst.zsearch.common.model;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * Kafka 消息中的原始文章数据，也是 legacy 管线中 raw_payload 的结构。
 * 必须是标准 POJO（无参构造 + getter/setter），供 Flink 识别为 POJO 类型。
 */
@Getter
@Setter
public class RawArticle {

    private String url;
    private String title;
    private String text;
    private String summary;
    private String name;
    private String source;
    private String homeUrl;
    private String avatar;
    private String createdAt;
    private String crawlerTime;
    private String emotion;
    private String riskLevel;
    private String ocr;
    private String asr;
    private Long repostCount;
    private Long commentCount;
    private Long likeCount;
    private Long followerCount;
    // 采集侧（MediaCrawler）历史字段名为 favoriteCount
    @JsonAlias("favoriteCount")
    private Long collectCount;
    private String siteId;
    private String userId;
    private List<String> imageList;
    private List<String> videoList;

    /**
     * 兼容 legacy raw_payload；Kafka 契约中以 IngestEnvelope.rawVersion 为准。
     */
    private Long rawVersion;
}
