package com.zst.zsearch.biz.model.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ArticleGroupByStatsDto extends ArticleQueryConditionDto {

    @NotBlank(message = "统计字段不能为空")
    private String field;

    @Min(value = 1, message = "topSize 必须大于等于 1")
    @Max(value = 100, message = "topSize 不能超过 100")
    private Integer topSize;
}
