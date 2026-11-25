package com.zst.zsearch.biz.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class SourceMonitorSettingSaveDto {

    private Long id;

    @NotNull(message = "所属分组不能为空")
    private Long groupId;

    @NotBlank(message = "监测设置名称不能为空")
    @Size(max = 30, message = "监测设置名称最长 30 个字符")
    private String name;

    @Size(max = 2000, message = "关键词最长 2000 个字符")
    private String keywords;

    @Size(max = 2000, message = "排除关键词最长 2000 个字符")
    private String excludeKeywords;

    @NotEmpty(message = "固定监测平台不能为空")
    private List<Long> siteIds;

    private List<String> uids;
}