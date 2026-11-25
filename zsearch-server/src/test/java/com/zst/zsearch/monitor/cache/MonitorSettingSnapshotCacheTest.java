package com.zst.zsearch.monitor.cache;

import com.zst.zsearch.biz.mapper.MonitorSettingMapper;
import com.zst.zsearch.biz.model.entity.MonitorSetting;
import com.zst.zsearch.common.enums.MonitorType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MonitorSettingSnapshotCacheTest {

    @Mock
    private MonitorSettingMapper monitorSettingMapper;

    @Test
    void loadsSnapshotAndServesItFromCache() {
        when(monitorSettingMapper.selectById(1L)).thenReturn(setting(1L, 2, "{\"siteIds\":[1]}"));

        MonitorSettingSnapshotCache cache = new MonitorSettingSnapshotCache(monitorSettingMapper);
        MonitorSettingSnapshot first = cache.get(1L);
        MonitorSettingSnapshot second = cache.get(1L);

        assertThat(first).isSameAs(second);
        assertThat(first.monitorType()).isEqualTo(MonitorType.SOURCE_MONITOR);
        assertThat(first.configJson()).isEqualTo("{\"siteIds\":[1]}");
        assertThat(first.configVersion()).isEqualTo(1);
        verify(monitorSettingMapper, times(1)).selectById(1L);
    }

    @Test
    void reloadsAfterInvalidation() {
        when(monitorSettingMapper.selectById(1L)).thenReturn(setting(1L, 1, "{}"));

        MonitorSettingSnapshotCache cache = new MonitorSettingSnapshotCache(monitorSettingMapper);
        cache.get(1L);
        cache.invalidate(1L);
        cache.get(1L);

        verify(monitorSettingMapper, times(2)).selectById(1L);
    }

    @Test
    void rejectsMissingSetting() {
        when(monitorSettingMapper.selectById(9L)).thenReturn(null);

        MonitorSettingSnapshotCache cache = new MonitorSettingSnapshotCache(monitorSettingMapper);

        assertThatThrownBy(() -> cache.get(9L))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("监测设置不存在");
    }

    private MonitorSetting setting(Long id, Integer type, String configJson) {
        MonitorSetting setting = new MonitorSetting();
        setting.setId(id);
        setting.setMonitorType(type);
        setting.setName("测试设置");
        setting.setConfigJson(configJson);
        setting.setConfigVersion(1);
        return setting;
    }
}