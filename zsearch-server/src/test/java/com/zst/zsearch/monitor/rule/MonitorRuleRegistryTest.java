package com.zst.zsearch.monitor.rule;

import com.zst.zsearch.common.enums.MonitorType;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MonitorRuleRegistryTest {

    @Test
    void rejectsDuplicateHandlers() {
        MonitorRuleHandler<?> first = mock(MonitorRuleHandler.class);
        MonitorRuleHandler<?> second = mock(MonitorRuleHandler.class);
        when(first.type()).thenReturn(MonitorType.SOURCE_MONITOR);
        when(second.type()).thenReturn(MonitorType.SOURCE_MONITOR);

        assertThatThrownBy(() -> new MonitorRuleRegistry(List.of(first, second)))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void rejectsMissingHandlerForSettingBackedType() {
        assertThatThrownBy(() -> new MonitorRuleRegistry(List.of(new KeywordMonitorRuleHandler())))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("SOURCE_MONITOR");
    }

    @Test
    void resolvesHandlerByType() {
        MonitorRuleRegistry registry = new MonitorRuleRegistry(List.of(
                new KeywordMonitorRuleHandler(),
                new SourceMonitorRuleHandler()));

        assertThat(registry.get(MonitorType.SOURCE_MONITOR)).isInstanceOf(SourceMonitorRuleHandler.class);
    }
}