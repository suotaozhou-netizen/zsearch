package com.zst.zsearch.util;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TimeRangeResolverTest {

    private final LocalDateTime now = LocalDateTime.of(2026, 11, 15, 12, 30);

    @Test
    void resolvesLastSevenDays() {
        TimeWindow window = TimeRangeResolver.resolve(
                TimeRangePreset.LAST_7_DAYS, null, null, now);

        assertThat(window.start()).isEqualTo(now.minusDays(7));
        assertThat(window.end()).isEqualTo(now);
    }

    @Test
    void resolvesTodayFromStartOfDay() {
        TimeWindow window = TimeRangeResolver.resolve(
                TimeRangePreset.TODAY, null, null, now);

        assertThat(window.start()).isEqualTo(now.toLocalDate().atStartOfDay());
        assertThat(window.end()).isEqualTo(now);
    }

    @Test
    void customEndDateIncludesWholeDay() {
        TimeWindow window = TimeRangeResolver.resolve(
                TimeRangePreset.CUSTOM, "2026-11-01", "2026-11-15", now);

        assertThat(window.start()).isEqualTo(LocalDateTime.of(2026, 11, 1, 0, 0));
        assertThat(window.end()).isEqualTo(LocalDateTime.of(2026, 11, 15, 23, 59, 59));
    }

    @Test
    void customRangeRequiresAtLeastOneBoundary() {
        assertThatThrownBy(() -> TimeRangeResolver.resolve(
                TimeRangePreset.CUSTOM, null, null, now))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("至少需要");
    }

    @Test
    void rejectsReversedCustomRange() {
        assertThatThrownBy(() -> TimeRangeResolver.resolve(
                TimeRangePreset.CUSTOM, "2026-11-15", "2026-11-01", now))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("不能晚于");
    }

    @Test
    void returnsNullWhenNoTimeConditionProvided() {
        assertThat(TimeRangeResolver.resolve(null, null, null, now)).isNull();
    }
}
