package com.zst.zsearch.es.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.Map;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class Article {
    //原文唯一id  有文章链接生成md5
    private String id;
    //标题
    private String title;
    //正文
    private String text;
    //AI摘要 后续由AI生成
    private String summary;
    //用户名
    private String name;
    //信息来源
    private String source;
    //链接
    private String url;
    //用户主页链接
    @JsonProperty("homeUrl")
    private String homeUrl;
    //用户主页头像
    private String avatar;
    //信息来源id
    @JsonProperty("siteId")
    private String siteId;
    //用户id
    @JsonProperty("userId")
    private String userId;
    //原文图片
    @JsonProperty("imageList")
    private List<String> imageList;
    //原文视频
    @JsonProperty("videoList")
    private List<String> videoList;
    //发布时间
    private String createdAt;
    //采集时间
    private String crawlerTime;
    //情感类型 后续由AI生成
    private String emotion;
    //风险等级 后续由AI生成
    private String riskLevel;
    //视频ocr 后续由AI生成
    private String ocr;
    //asr    后续由AI生成
    private String asr;
    //转发数
    private Long repostCount;
    //评论数
    private Long commentCount;
    //点赞数
    private Long likeCount;
    //粉丝数
    private Long followerCount;
    //收藏数
    private Long collectCount;
    //互动量总数 由 repostCount + commentCount + likeCount + followerCount + collectCount计算
    private Long interactionCount;
    //正文长度
    private Integer textLength;

    private Double score;
    private Map<String, List<String>> highlight;
}
