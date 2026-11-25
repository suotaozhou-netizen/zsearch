package com.zst.zsearch.biz.model.vo;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class ArticleSumStatsVo {

    private long total;
    private long took;
    private ArticleInteractionSumVo sums;
    private List<ArticleEmotionSumVo> byEmotion;
}
