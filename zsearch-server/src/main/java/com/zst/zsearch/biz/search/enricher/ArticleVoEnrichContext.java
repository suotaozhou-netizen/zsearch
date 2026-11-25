package com.zst.zsearch.biz.search.enricher;

import java.util.List;

public record ArticleVoEnrichContext(
        Long settingId,
        String settingName,
        String userId,
        List<String> keywords
) {

    public ArticleVoEnrichContext {
        keywords = keywords == null ? List.of() : List.copyOf(keywords);
    }

    public static ArticleVoEnrichContext empty() {
        return new ArticleVoEnrichContext(null, null, null, List.of());
    }
}
