package com.zst.zsearch.search.service;

import com.zst.zsearch.common.enums.MonitorType;
import com.zst.zsearch.es.dto.ArticleSearchRequest;
import com.zst.zsearch.es.dto.ArticleSearchResponse;
import com.zst.zsearch.es.service.ArticleSearchService;
import com.zst.zsearch.search.assembler.ArticleSearchRequestAssembler;
import com.zst.zsearch.search.command.GeneralSearchCommand;
import com.zst.zsearch.search.model.SearchScope;
import com.zst.zsearch.search.model.SearchSpec;
import com.zst.zsearch.search.resolver.SearchSpecResolverRegistry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SearchApplicationServiceTest {

    @Mock
    private SearchSpecResolverRegistry resolverRegistry;

    @Mock
    private ArticleSearchRequestAssembler requestAssembler;

    @Mock
    private ArticleSearchService articleSearchService;

    @InjectMocks
    private SearchApplicationService searchApplicationService;

    @Test
    void resolvesAssemblesAndExecutesSearch() {
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
        ArticleSearchRequest request = new ArticleSearchRequest();
        ArticleSearchResponse response = new ArticleSearchResponse();
        when(resolverRegistry.resolve(command)).thenReturn(spec);
        when(requestAssembler.assemble(spec)).thenReturn(request);
        when(articleSearchService.search(request)).thenReturn(response);

        ArticleSearchResponse actual = searchApplicationService.search(command);

        assertThat(actual).isSameAs(response);
        verify(resolverRegistry).resolve(command);
        verify(requestAssembler).assemble(spec);
        verify(articleSearchService).search(request);
    }
}
