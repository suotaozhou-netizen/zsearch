package com.zst.zsearch.util;

import java.util.Locale;

public enum TimeRangePreset {

    TODAY,
    LAST_24_HOURS,
    LAST_3_DAYS,
    LAST_7_DAYS,
    LAST_1_MONTH,
    LAST_3_MONTHS,
    CUSTOM;

    public static TimeRangePreset from(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("不支持的监测时间: " + value);
        }
    }
}
