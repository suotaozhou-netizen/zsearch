package com.zst.zsearch.biz.model.vo;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class ArticleTrendStatsVo {

    private long total;
    private long took;
    private String timeField;
    private String interval;
    private List<ArticleTrendBucketVo> buckets;
}
