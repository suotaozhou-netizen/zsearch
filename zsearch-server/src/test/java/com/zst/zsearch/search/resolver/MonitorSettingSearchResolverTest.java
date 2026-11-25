package com.zst.zsearch.search.resolver;

import com.zst.zsearch.es.query.FieldTermFilter;
import com.zst.zsearch.common.enums.MonitorType;
import com.zst.zsearch.monitor.cache.MonitorSettingSnapshot;
import com.zst.zsearch.monitor.cache.MonitorSettingSnapshotCache;
import com.zst.zsearch.monitor.rule.KeywordMonitorRuleHandler;
import com.zst.zsearch.monitor.rule.MonitorRuleRegistry;
import com.zst.zsearch.monitor.rule.SourceMonitorRuleHandler;
import com.zst.zsearch.search.command.MonitorSettingSearchCommand;
import com.zst.zsearch.search.model.SearchCriteria;
import com.zst.zsearch.search.model.SearchSpec;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MonitorSettingSearchResolverTest {

    @Mock
    private MonitorSettingSnapshotCache snapshotCache;

    private MonitorSettingSearchResolver resolver;

    @BeforeEach
    void setUp() {
        MonitorRuleRegistry ruleRegistry = new MonitorRuleRegistry(List.of(
                new KeywordMonitorRuleHandler(),
                new SourceMonitorRuleHandler()));
        resolver = new MonitorSettingSearchResolver(snapshotCache, ruleRegistry, new SearchSpecNormalizer());
    }

    @Test
    void combinesStoredKeywordsWithRuntimeFilters() {
        when(snapshotCache.get(1L)).thenReturn(new MonitorSettingSnapshot(
                1L, MonitorType.KEYWORD_MONITOR, "深圳+(男生|男性)", "招聘|广告", "{}", 1));
        SearchCriteria criteria = new SearchCriteria(
                "FULL_TEXT", null, null, null,
                null, List.of("负面"), List.of("关注风险"),
                1, 20, "created_at", "desc", true);

        SearchSpec spec = resolver.resolve(new MonitorSettingSearchCommand(1L, criteria));

        assertThat(resolver.commandType()).isEqualTo(MonitorSettingSearchCommand.class);
        assertThat(spec.monitorType()).isEqualTo(MonitorType.KEYWORD_MONITOR);
        assertThat(spec.keywords()).isEqualTo("深圳+(男生|男性)");
        assertThat(spec.excludeKeywords()).isEqualTo("招聘|广告");
        assertThat(spec.nearSlop()).isEqualTo(7);
        assertThat(spec.emotions()).containsExactly("负面");
        assertThat(spec.riskLevels()).containsExactly("关注风险");
        assertThat(spec.filters()).isEmpty();
    }

    @Test
    void appendsSourceSiteFilterForSourceMonitor() {
        when(snapshotCache.get(2L)).thenReturn(new MonitorSettingSnapshot(
                2L, MonitorType.SOURCE_MONITOR, null, null, "{\"siteIds\":[1,4]}", 1));

        SearchSpec spec = resolver.resolve(new MonitorSettingSearchCommand(2L, null));

        assertThat(spec.monitorType()).isEqualTo(MonitorType.SOURCE_MONITOR);
        assertThat(spec.keywords()).isNull();
        assertThat(spec.filters()).containsExactly(
                new FieldTermFilter("siteId", List.of("1", "4")));
    }

    @Test
    void propagatesMissingMonitorSetting() {
        when(snapshotCache.get(9L)).thenThrow(
                new ResponseStatusException(HttpStatus.NOT_FOUND, "监测设置不存在: 9"));

        assertThatThrownBy(() -> resolver.resolve(new MonitorSettingSearchCommand(9L, null)))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("监测设置不存在");
    }
}