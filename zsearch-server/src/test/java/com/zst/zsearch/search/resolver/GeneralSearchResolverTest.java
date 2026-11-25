package com.zst.zsearch.search.resolver;

import com.zst.zsearch.common.enums.MonitorType;
import com.zst.zsearch.search.command.GeneralSearchCommand;
import com.zst.zsearch.search.model.SearchCriteria;
import com.zst.zsearch.search.model.SearchScope;
import com.zst.zsearch.search.model.SearchSpec;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class GeneralSearchResolverTest {

    private final GeneralSearchResolver resolver =
            new GeneralSearchResolver(new SearchSpecNormalizer());

    @Test
    void resolvesFrontendSearchIntoNormalizedSpec() {
        SearchCriteria criteria = new SearchCriteria(
                "TITLE",
                null,
                "2026-11-01T08:00:00",
                "2026-11-02T08:00:00",
                List.of("微博"),
                List.of("负面"),
                List.of("关注风险"),
                2,
                10,
                "_score",
                "desc",
                true
        );
        GeneralSearchCommand command = new GeneralSearchCommand(
                "深圳+(男生|男性)", "招聘|广告", 5, criteria);

        SearchSpec spec = resolver.resolve(command);

        assertThat(resolver.commandType()).isEqualTo(GeneralSearchCommand.class);
        assertThat(spec.monitorType()).isEqualTo(MonitorType.GENERAL_QUERY);
        assertThat(spec.keywords()).isEqualTo("深圳+(男生|男性)");
        assertThat(spec.excludeKeywords()).isEqualTo("招聘|广告");
        assertThat(spec.nearSlop()).isEqualTo(5);
        assertThat(spec.searchScope()).isEqualTo(SearchScope.TITLE);
        assertThat(spec.timeWindow().start()).isEqualTo(LocalDateTime.of(2026, 11, 1, 8, 0));
        assertThat(spec.timeWindow().end()).isEqualTo(LocalDateTime.of(2026, 11, 2, 8, 0));
        assertThat(spec.sources()).containsExactly("微博");
        assertThat(spec.emotions()).containsExactly("负面");
        assertThat(spec.riskLevels()).containsExactly("关注风险");
        assertThat(spec.filters()).isEmpty();
        assertThat(spec.page()).isEqualTo(2);
        assertThat(spec.size()).isEqualTo(10);
        assertThat(spec.sortBy()).isEqualTo("_score");
    }
}
