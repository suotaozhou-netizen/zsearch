package com.zst.zsearch.biz.model.vo;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class SourceMonitorGroupTreeVo {

    private Long id;
    private String name;
    private String createTime;
    private String updateTime;
    private List<SourceMonitorSettingVo> settings;
}