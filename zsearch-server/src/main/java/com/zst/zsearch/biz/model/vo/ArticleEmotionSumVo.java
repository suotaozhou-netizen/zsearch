package com.zst.zsearch.biz.model.vo;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ArticleEmotionSumVo {

    private String emotion;
    private long count;
    private ArticleInteractionSumVo sums;
}
