package com.zst.zsearch.service;

import com.zst.zsearch.biz.service.MonitorService;
import com.zst.zsearch.es.query.FieldTermFilter;
import com.zst.zsearch.biz.mapper.MonitorGroupMapper;
import com.zst.zsearch.biz.mapper.MonitorSettingMapper;
import com.zst.zsearch.biz.model.dto.MonitorSettingSaveDto;
import com.zst.zsearch.biz.model.entity.MonitorGroup;
import com.zst.zsearch.biz.model.entity.MonitorSetting;
import com.zst.zsearch.common.enums.MonitorType;
import com.zst.zsearch.monitor.cache.MonitorSettingSnapshotCache;
import com.zst.zsearch.monitor.rule.MonitorRuleHandler;
import com.zst.zsearch.monitor.rule.MonitorRuleRegistry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MonitorServiceTest {

    @Mock
    private MonitorGroupMapper monitorGroupMapper;

    @Mock
    private MonitorSettingMapper monitorSettingMapper;

    @Mock
    private MonitorRuleRegistry monitorRuleRegistry;

    @Mock
    private MonitorSettingSnapshotCache snapshotCache;

    @InjectMocks
    private MonitorService monitorService;

    @Test
    void rejectsSettingWhenGroupDoesNotExist() {
        MonitorSettingSaveDto request = new MonitorSettingSaveDto();
        request.setGroupId(99L);
        request.setName("测试设置");
        request.setKeywords("深圳");
        when(monitorGroupMapper.selectById(99L)).thenReturn(null);

        assertThatThrownBy(() -> monitorService.createSetting(request))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("监测分组不存在");
        verify(monitorSettingMapper, never()).insert(any(MonitorSetting.class));
    }

    @Test
    void rejectsKeywordMonitorWithoutKeywords() {
        when(monitorGroupMapper.selectById(1L)).thenReturn(new MonitorGroup());
        MonitorSettingSaveDto request = settingDto(MonitorType.KEYWORD_MONITOR, "深圳舆情");
        request.setKeywords(null);

        assertThatThrownBy(() -> monitorService.createSetting(request))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("关键词不能为空");
        verify(monitorSettingMapper, never()).insert(any(MonitorSetting.class));
    }

    @Test
    void rejectsGeneralQueryAsSettingType() {
        when(monitorGroupMapper.selectById(1L)).thenReturn(new MonitorGroup());
        MonitorSettingSaveDto request = settingDto(MonitorType.GENERAL_QUERY, "通用查询");
        request.setKeywords(null);

        assertThatThrownBy(() -> monitorService.createSetting(request))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("不支持创建监测设置");
    }

    @Test
    void createsSourceMonitorSettingWithoutKeywords() {
        when(monitorGroupMapper.selectById(1L)).thenReturn(new MonitorGroup());
        when(monitorSettingMapper.selectCount(any())).thenReturn(0L);
        MonitorRuleHandler<?> handler = mock(MonitorRuleHandler.class);
        doReturn(handler).when(monitorRuleRegistry).get(MonitorType.SOURCE_MONITOR);
        when(handler.filtersOf(eq(1), anyString()))
                .thenReturn(List.of(new FieldTermFilter("siteId", List.of("1", "4"))));

        MonitorSettingSaveDto request = settingDto(MonitorType.SOURCE_MONITOR, "双平台监测");
        request.setKeywords(null);
        request.setConfig(Map.of("siteIds", List.of(1, 4)));

        monitorService.createSetting(request);

        ArgumentCaptor<MonitorSetting> captor = ArgumentCaptor.forClass(MonitorSetting.class);
        verify(monitorSettingMapper).insert(captor.capture());
        MonitorSetting setting = captor.getValue();
        assertThat(setting.getMonitorType()).isEqualTo(MonitorType.SOURCE_MONITOR.getCode());
        assertThat(setting.getConfigJson()).isEqualTo("{\"siteIds\":[1,4]}");
        assertThat(setting.getConfigVersion()).isEqualTo(1);
        assertThat(setting.getKeywords()).isNull();
    }

    @Test
    void rejectsDeletingGroupWithSettings() {
        when(monitorGroupMapper.selectById(1L)).thenReturn(new MonitorGroup());
        when(monitorSettingMapper.selectCount(any())).thenReturn(1L);

        assertThatThrownBy(() -> monitorService.deleteGroup(1L))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("不能删除");
        verify(monitorGroupMapper, never()).deleteById(1L);
    }

    private MonitorSettingSaveDto settingDto(MonitorType type, String name) {
        MonitorSettingSaveDto request = new MonitorSettingSaveDto();
        request.setGroupId(1L);
        request.setMonitorType(type.getCode());
        request.setName(name);
        request.setKeywords("深圳");
        return request;
    }
}