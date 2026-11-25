package com.zst.zsearch.biz.model.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class ArticleQueryConditionDto {

    private Long settingId;

    @NotNull(message = "监测类型不能为空")
    private Integer monitorType;

    private String keywords;

    private String excludeKeywords;

    @Min(value = 0, message = "nearSlop 不能小于 0")
    @Max(value = 20, message = "nearSlop 不能超过 20")
    private Integer nearSlop;

    private String searchScope;

    private String startTime;

    private String endTime;

    private List<String> sources;

    private List<String> emotions;

    private List<String> riskLevels;
}
