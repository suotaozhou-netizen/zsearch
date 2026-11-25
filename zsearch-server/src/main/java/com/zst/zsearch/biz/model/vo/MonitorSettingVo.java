package com.zst.zsearch.biz.model.vo;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MonitorSettingVo {

    private Long id;
    private Long groupId;
    private String name;
    private String keywords;
    private String excludeKeywords;
    private Integer status;
    private Integer createUserId;
    private String createUserName;
    private String createTime;
    private String updateTime;
}