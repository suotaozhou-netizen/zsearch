package com.zst.zsearch.es.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class ArticleTrendStatsResponse {

    private long total;
    private long took;
    private String timeField;
    private String interval;
    private List<TrendBucket> buckets;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TrendBucket {

        private String key;
        private String keyAsString;
        private long count;
        private long interactionCount;
        private List<TermBucket> siteCounts;
    }
}
