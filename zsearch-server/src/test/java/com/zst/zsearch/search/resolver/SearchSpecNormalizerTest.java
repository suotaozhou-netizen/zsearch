package com.zst.zsearch.search.resolver;

import com.zst.zsearch.es.query.FieldTermFilter;
import com.zst.zsearch.common.enums.MonitorType;
import com.zst.zsearch.search.model.SearchCriteria;
import com.zst.zsearch.search.model.SearchScope;
import com.zst.zsearch.search.model.SearchSpec;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SearchSpecNormalizerTest {

    private final SearchSpecNormalizer normalizer = new SearchSpecNormalizer();

    @Test
    void appliesDefaultSearchCriteria() {
        SearchSpec spec = normalizer.normalize(
                MonitorType.GENERAL_QUERY, " 深圳 ", null, null, null, List.of());

        assertThat(spec.monitorType()).isEqualTo(MonitorType.GENERAL_QUERY);
        assertThat(spec.keywords()).isEqualTo("深圳");
        assertThat(spec.excludeKeywords()).isNull();
        assertThat(spec.nearSlop()).isEqualTo(7);
        assertThat(spec.searchScope()).isEqualTo(SearchScope.FULL_TEXT);
        assertThat(spec.timeWindow()).isNull();
        assertThat(spec.sources()).isEmpty();
        assertThat(spec.page()).isEqualTo(1);
        assertThat(spec.size()).isEqualTo(20);
        assertThat(spec.sortBy()).isEqualTo("created_at");
        assertThat(spec.sortOrder()).isEqualTo("desc");
        assertThat(spec.highlight()).isTrue();
    }

    @Test
    void normalizesCustomTimeAndRuntimeFilters() {
        SearchCriteria criteria = new SearchCriteria(
                "TITLE",
                "CUSTOM",
                "2026-11-01",
                "2026-11-14",
                List.of(" 微博 ", " "),
                List.of("负面"),
                List.of("关注风险"),
                2,
                50,
                "crawler_time",
                "ASC",
                false
        );

        SearchSpec spec = normalizer.normalize(
                MonitorType.GENERAL_QUERY, "深圳", "招聘|广告", 5, criteria, List.of());

        assertThat(spec.searchScope()).isEqualTo(SearchScope.TITLE);
        assertThat(spec.nearSlop()).isEqualTo(5);
        assertThat(spec.timeWindow().start()).isEqualTo(LocalDateTime.of(2026, 11, 1, 0, 0));
        assertThat(spec.timeWindow().end()).isEqualTo(LocalDateTime.of(2026, 11, 14, 23, 59, 59));
        assertThat(spec.sources()).containsExactly("微博");
        assertThat(spec.emotions()).containsExactly("负面");
        assertThat(spec.riskLevels()).containsExactly("关注风险");
        assertThat(spec.page()).isEqualTo(2);
        assertThat(spec.size()).isEqualTo(50);
        assertThat(spec.sortBy()).isEqualTo("crawler_time");
        assertThat(spec.sortOrder()).isEqualTo("asc");
        assertThat(spec.highlight()).isFalse();
    }

    @Test
    void rejectsExcludeKeywordsWithoutKeywords() {
        assertThatThrownBy(() -> normalizer.normalize(
                MonitorType.GENERAL_QUERY, null, "招聘|广告", null, null, List.of()))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("排除关键词必须与关键词一起使用");
    }

    @Test
    void rejectsUnknownSearchScope() {
        SearchCriteria criteria = new SearchCriteria(
                "BODY", null, null, null, null, null, null, 1, 20, "created_at", "desc", true);

        assertThatThrownBy(() -> normalizer.normalize(
                MonitorType.GENERAL_QUERY, "深圳", null, null, criteria, List.of()))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("不支持的搜索方式");
    }

    @Test
    void rejectsInvalidNearSlop() {
        assertThatThrownBy(() -> normalizer.normalize(
                MonitorType.GENERAL_QUERY, "深圳", null, 21, null, List.of()))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("nearSlop");
    }

    @Test
    void carriesMandatoryMonitorFilters() {
        FieldTermFilter filter = new FieldTermFilter("siteId", List.of("1", "4"));

        SearchSpec spec = normalizer.normalize(
                MonitorType.SOURCE_MONITOR, null, null, null, null, List.of(filter));

        assertThat(spec.monitorType()).isEqualTo(MonitorType.SOURCE_MONITOR);
        assertThat(spec.filters()).containsExactly(filter);
    }
}
