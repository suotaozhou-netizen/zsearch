package com.zst.zsearch.common.enums;

import lombok.Getter;

@Getter
public enum MonitorType {

    GENERAL_QUERY(0, "通用查询", false),
    KEYWORD_MONITOR(1, "关键词监测", true),
    SOURCE_MONITOR(2, "信息源监测", true);

    private final int code;
    private final String description;
    private final boolean settingBacked;

    MonitorType(int code, String description, boolean settingBacked) {
        this.code = code;
        this.description = description;
        this.settingBacked = settingBacked;
    }

    public static MonitorType fromCode(int code) {
        for (MonitorType type : values()) {
            if (type.code == code) {
                return type;
            }
        }
        throw new IllegalArgumentException("不支持的监测类型: " + code);
    }
}