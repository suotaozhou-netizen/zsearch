package com.zst.zsearch.biz.model.vo;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class SourceMonitorSettingVo {

    private Long id;
    private Long groupId;
    private String name;
    private String keywords;
    private String excludeKeywords;
    private List<Long> siteIds;
    private List<String> uids;
    private Integer status;
    private Integer createUserId;
    private String createUserName;
    private String createTime;
    private String updateTime;
}