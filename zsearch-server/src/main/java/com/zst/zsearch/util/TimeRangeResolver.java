package com.zst.zsearch.util;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

public final class TimeRangeResolver {

    private static final ZoneId ZONE = ZoneId.of("Asia/Shanghai");
    private static final DateTimeFormatter DATE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private TimeRangeResolver() {
    }

    public static LocalDateTime now() {
        return LocalDateTime.now(ZONE);
    }

    public static TimeWindow resolve(TimeRangePreset preset, String startTime, String endTime,
                                     LocalDateTime now) {
        if (preset != null && preset != TimeRangePreset.CUSTOM) {
            return new TimeWindow(presetStart(preset, now), now);
        }

        LocalDateTime start = parseDateTime(startTime, false);
        LocalDateTime end = parseDateTime(endTime, true);
        if (preset == TimeRangePreset.CUSTOM && start == null && end == null) {
            throw new IllegalArgumentException("自定义监测时间至少需要 startTime 或 endTime");
        }
        if (start != null && end != null && start.isAfter(end)) {
            throw new IllegalArgumentException("startTime 不能晚于 endTime");
        }
        if (start == null && end == null) {
            return null;
        }
        return new TimeWindow(start, end);
    }

    private static LocalDateTime presetStart(TimeRangePreset preset, LocalDateTime now) {
        return switch (preset) {
            case TODAY -> now.toLocalDate().atStartOfDay();
            case LAST_24_HOURS -> now.minusHours(24);
            case LAST_3_DAYS -> now.minusDays(3);
            case LAST_7_DAYS -> now.minusDays(7);
            case LAST_1_MONTH -> now.minusMonths(1);
            case LAST_3_MONTHS -> now.minusMonths(3);
            case CUSTOM -> throw new IllegalArgumentException("CUSTOM 不能使用预设时间");
        };
    }

    private static LocalDateTime parseDateTime(String value, boolean endOfDay) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String text = value.trim();
        try {
            if (text.length() == 10) {
                LocalDate date = LocalDate.parse(text);
                return endOfDay ? date.atTime(LocalTime.of(23, 59, 59)) : date.atStartOfDay();
            }
            if (text.indexOf('T') > 0) {
                return LocalDateTime.parse(text);
            }
            return LocalDateTime.parse(text, DATE_TIME_FORMATTER);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException(
                    "时间格式应为 yyyy-MM-dd、yyyy-MM-dd HH:mm:ss 或 ISO-8601");
        }
    }
}
