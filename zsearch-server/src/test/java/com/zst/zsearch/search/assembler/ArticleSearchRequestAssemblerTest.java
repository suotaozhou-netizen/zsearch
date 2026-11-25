package com.zst.zsearch.search.assembler;

import com.zst.zsearch.common.enums.MonitorType;
import com.zst.zsearch.es.dto.ArticleSearchRequest;
import com.zst.zsearch.search.model.SearchScope;
import com.zst.zsearch.search.model.SearchSpec;
import com.zst.zsearch.util.TimeWindow;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ArticleSearchRequestAssemblerTest {

    private final ArticleSearchRequestAssembler assembler = new ArticleSearchRequestAssembler();

    @Test
    void mapsSpecToElasticsearchRequest() {
        SearchSpec spec = new SearchSpec(
                MonitorType.KEYWORD_MONITOR,
                "深圳+(男生|男性)",
                "招聘|广告",
                5,
                SearchScope.TITLE,
                new TimeWindow(
                        LocalDateTime.of(2026, 11, 1, 0, 0),
                        LocalDateTime.of(2026, 11, 14, 23, 59, 59)),
                List.of("微博"),
                List.of("负面"),
                List.of("关注风险"),
                List.of(),
                2,
                50,
                "crawler_time",
                "asc",
                false
        );

        ArticleSearchRequest request = assembler.assemble(spec);

        assertThat(request.getKeywords()).isEqualTo("深圳+(男生|男性)");
        assertThat(request.getExcludeKeywords()).isEqualTo("招聘|广告");
        assertThat(request.getNearSlop()).isEqualTo(5);
        assertThat(request.getSearchScope()).isEqualTo("TITLE");
        assertThat(request.getStartTime()).isEqualTo("2026-11-01T00:00:00");
        assertThat(request.getEndTime()).isEqualTo("2026-11-14T23:59:59");
        assertThat(request.getSources()).containsExactly("微博");
        assertThat(request.getEmotions()).containsExactly("负面");
        assertThat(request.getRiskLevels()).containsExactly("关注风险");
        assertThat(request.getFilters()).isEmpty();
        assertThat(request.getPage()).isEqualTo(2);
        assertThat(request.getSize()).isEqualTo(50);
        assertThat(request.getSortBy()).isEqualTo("crawler_time");
        assertThat(request.getSortOrder()).isEqualTo("asc");
        assertThat(request.getHighlight()).isFalse();
    }

    @Test
    void leavesTimeRangeEmptyWhenSpecHasNoWindow() {
        SearchSpec spec = new SearchSpec(
                MonitorType.GENERAL_QUERY,
                "深圳",
                null,
                7,
                SearchScope.FULL_TEXT,
                null,
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                1,
                20,
                "created_at",
                "desc",
                true
        );

        ArticleSearchRequest request = assembler.assemble(spec);

        assertThat(request.getStartTime()).isNull();
        assertThat(request.getEndTime()).isNull();
    }
}
