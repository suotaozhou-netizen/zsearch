package com.zst.zsearch.biz.model.vo;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class ArticleOverviewStatsVo {

    private long total;
    private long took;
    private List<ArticleTermCountVo> sources;
    private List<ArticleTermCountVo> emotions;
    private List<ArticleTermCountVo> riskLevels;
}
