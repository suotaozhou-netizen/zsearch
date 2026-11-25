package com.zst.zsearch.biz.model.dto;

import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ArticleSiteTrendStatsDto extends ArticleQueryConditionDto {

    @Pattern(regexp = "(?i)hour|day|month", message = "统计间隔仅支持 hour、day、month")
    private String interval;
}