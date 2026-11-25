package com.zst.zsearch.biz.model.vo;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class ArticleTrendBucketVo {

    private String key;
    private String keyAsString;
    private long count;
    private long interactionCount;
    private List<ArticleTermCountVo> siteCounts;
}
