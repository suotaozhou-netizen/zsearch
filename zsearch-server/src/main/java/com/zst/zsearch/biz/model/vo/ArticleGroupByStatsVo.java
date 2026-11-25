package com.zst.zsearch.biz.model.vo;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class ArticleGroupByStatsVo {

    private long total;
    private long took;
    private String field;
    private List<ArticleTermCountVo> buckets;
}
