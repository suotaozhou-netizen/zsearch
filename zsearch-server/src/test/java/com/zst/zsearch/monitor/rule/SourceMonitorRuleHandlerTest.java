package com.zst.zsearch.monitor.rule;

import com.zst.zsearch.common.enums.SiteId;
import com.zst.zsearch.es.query.FieldTermFilter;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SourceMonitorRuleHandlerTest {

    private final SourceMonitorRuleHandler handler = new SourceMonitorRuleHandler();

    @Test
    void parsesAndDeduplicatesSiteIds() {
        SourceMonitorRule rule = handler.parse(1, "{\"siteIds\":[1,4,1]}");

        assertThat(rule.siteIds()).containsExactly(SiteId.DOUYIN, SiteId.WEIBO);
    }

    @Test
    void buildsSiteIdTermsFilter() {
        List<FieldTermFilter> filters = handler.toFilters(
                new SourceMonitorRule(List.of(SiteId.DOUYIN, SiteId.WEIBO)));

        assertThat(filters).containsExactly(
                new FieldTermFilter("siteId", List.of("1", "4")));
    }

    @Test
    void rejectsMissingSiteIds() {
        assertThatThrownBy(() -> handler.parse(1, "{}"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("至少选择一个平台");
    }

    @Test
    void rejectsUnknownSiteId() {
        assertThatThrownBy(() -> handler.parse(1, "{\"siteIds\":[99]}"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("不支持的站点id");
    }

    @Test
    void rejectsUnsupportedConfigVersion() {
        assertThatThrownBy(() -> handler.parse(2, "{\"siteIds\":[1]}"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("不支持的配置版本");
    }

    @Test
    void rejectsMalformedConfig() {
        assertThatThrownBy(() -> handler.parse(1, "{oops"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("配置格式不正确");
    }
}