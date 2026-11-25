package com.zst.zsearch.biz.model.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ArticleTrendStatsDto extends ArticleQueryConditionDto {

    private String timeField;

    private String interval;
}
