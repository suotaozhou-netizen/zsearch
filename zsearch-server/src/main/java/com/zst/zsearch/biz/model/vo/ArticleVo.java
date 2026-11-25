package com.zst.zsearch.biz.model.vo;

import lombok.Getter;
import lombok.Setter;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Getter
@Setter
public class ArticleVo {

    private String id;
    private String title;
    private String text;
    private String summary;
    private String name;
    private String source;
    private String url;
    private String homeUrl;
    private String avatar;
    private String siteId;
    private String userId;
    private List<String> imageList;
    private List<String> videoList;
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
    private Long collectCount;
    private Long interactionCount;

    // 非 ES 字段
    private Long settingId;
    private String settingName;
    private Boolean read = false;
    // 后端统计的关键词命中次数，如 {"都市丽人": 1}
    private Map<String, Integer> highlight = new LinkedHashMap<>();
}
