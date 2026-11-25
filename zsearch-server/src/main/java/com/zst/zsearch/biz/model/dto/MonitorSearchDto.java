package com.zst.zsearch.biz.model.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MonitorSearchDto extends ArticleQueryConditionDto {

    @Min(value = 1, message = "page 必须大于等于 1")
    private Integer page;

    @Min(value = 1, message = "size 必须大于等于 1")
    @Max(value = 100, message = "size 不能超过 100")
    private Integer size;

    private String sortBy;

    private String sortOrder;
}
