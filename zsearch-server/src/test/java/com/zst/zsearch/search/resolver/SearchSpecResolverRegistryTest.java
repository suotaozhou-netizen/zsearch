package com.zst.zsearch.search.resolver;

import com.zst.zsearch.common.enums.MonitorType;
import com.zst.zsearch.search.command.GeneralSearchCommand;
import com.zst.zsearch.search.model.SearchScope;
import com.zst.zsearch.search.model.SearchSpec;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SearchSpecResolverRegistryTest {

    @Test
    @SuppressWarnings("unchecked")
    void dispatchesByCommandType() {
        GeneralSearchCommand command = new GeneralSearchCommand("深圳", null, 7, null);
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
        SearchSpecResolver<GeneralSearchCommand> resolver = mock(SearchSpecResolver.class);
        when(resolver.commandType()).thenReturn(GeneralSearchCommand.class);
        when(resolver.resolve(command)).thenReturn(spec);
        SearchSpecResolverRegistry registry = new SearchSpecResolverRegistry(List.of(resolver));

        assertThat(registry.resolve(command)).isSameAs(spec);
    }

    @Test
    void rejectsUnsupportedCommand() {
        SearchSpecResolverRegistry registry = new SearchSpecResolverRegistry(List.of());
        GeneralSearchCommand command = new GeneralSearchCommand("深圳", null, 7, null);

        assertThatThrownBy(() -> registry.resolve(command))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("不支持的检索命令");
    }
}