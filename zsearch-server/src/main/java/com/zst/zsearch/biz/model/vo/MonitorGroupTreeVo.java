package com.zst.zsearch.biz.model.vo;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class MonitorGroupTreeVo {

    private Long id;
    private String name;
    private String createTime;
    private String updateTime;
    private List<MonitorSettingVo> settings;
}