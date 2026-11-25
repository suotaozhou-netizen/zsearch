package com.zst.zsearch.common.enums;

import lombok.Getter;

@Getter
public enum SiteId {

    DOUYIN(1, "抖音"),
    XIAOHONGSHU(2, "小红书"),
    BILIBILI(3, "哔哩哔哩"),
    WEIBO(4, "微博"),
    TOUTIAO(5, "今日头条"),
    KUAISHOU(6, "快手"),
    TIEBA(7, "贴吧"),
    ZHIHU(8, "知乎");

    private final int code;
    private final String description;

    SiteId(int code, String description) {
        this.code = code;
        this.description = description;
    }

    public static SiteId fromCode(int code) {
        SiteId siteId = fromCodeOrNull(code);
        if (siteId == null) {
            throw new IllegalArgumentException("不支持的站点id: " + code);
        }
        return siteId;
    }

    /**
     * 容错版本：未知编码返回 null，不抛异常。
     * 用于展示或历史数据兼容场景，校验场景请使用 {@link #fromCode(int)}。
     */
    public static SiteId fromCodeOrNull(int code) {
        for (SiteId siteId : values()) {
            if (siteId.code == code) {
                return siteId;
            }
        }
        return null;
    }
}
