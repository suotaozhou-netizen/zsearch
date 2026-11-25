package com.zst.zsearch.biz.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SourceMonitorGroupSaveDto {

    private Long id;

    @NotBlank(message = "分组名称不能为空")
    @Size(max = 50, message = "分组名称最长 50 个字符")
    private String name;
}