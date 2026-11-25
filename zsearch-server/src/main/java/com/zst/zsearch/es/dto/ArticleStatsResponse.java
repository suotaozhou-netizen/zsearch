package com.zst.zsearch.es.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class ArticleStatsResponse {

    private long total;
    private long took;
    private List<TermBucket> sources;
    private List<TermBucket> emotions;
    private List<TermBucket> riskLevels;
}
