package com.zst.zsearch.biz.model.support;

import com.alibaba.fastjson2.JSON;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

/**
 * monitor_setting 中以字符串保存的多值字段（siteIds/uids）与集合之间的转换。
 * 写库统一使用逗号分隔；读取时兼容历史 JSON 数组格式。
 */
public final class MonitorSettingValues {

    private MonitorSettingValues() {
    }

    public static String join(Collection<?> values) {
        if (values == null || values.isEmpty()) {
            return null;
        }
        List<String> normalized = values.stream()
                .filter(Objects::nonNull)
                .map(String::valueOf)
                .map(String::trim)
                .filter(StringUtils::hasText)
                .distinct()
                .toList();
        return normalized.isEmpty() ? null : String.join(",", normalized);
    }

    public static List<String> split(String raw) {
        if (!StringUtils.hasText(raw)) {
            return List.of();
        }
        String value = raw.trim();
        if (value.startsWith("[")) {
            List<String> parsed = JSON.parseArray(value, String.class);
            return parsed == null ? List.of() : normalize(parsed);
        }
        return normalize(Arrays.asList(value.split(",")));
    }

    public static List<Long> splitToLongs(String raw) {
        return split(raw).stream()
                .map(Long::valueOf)
                .toList();
    }

    private static List<String> normalize(List<String> values) {
        return values.stream()
                .filter(StringUtils::hasText)
                .map(String::trim)
                .distinct()
                .toList();
    }
}