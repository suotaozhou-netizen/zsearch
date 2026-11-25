package com.zst.zsearch.enums;

import com.zst.zsearch.common.enums.MonitorType;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MonitorTypeTest {

    @Test
    void mapsCodesToMonitorTypes() {
        assertThat(MonitorType.fromCode(0)).isEqualTo(MonitorType.GENERAL_QUERY);
        assertThat(MonitorType.fromCode(1)).isEqualTo(MonitorType.KEYWORD_MONITOR);
        assertThat(MonitorType.fromCode(2)).isEqualTo(MonitorType.SOURCE_MONITOR);
    }

    @Test
    void marksOnlyPersistedTypesAsSettingBacked() {
        assertThat(MonitorType.GENERAL_QUERY.isSettingBacked()).isFalse();
        assertThat(MonitorType.KEYWORD_MONITOR.isSettingBacked()).isTrue();
        assertThat(MonitorType.SOURCE_MONITOR.isSettingBacked()).isTrue();
    }

    @Test
    void rejectsUnknownCode() {
        assertThatThrownBy(() -> MonitorType.fromCode(3))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("不支持的监测类型");
    }
}