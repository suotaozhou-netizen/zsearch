package com.zst.zsearch.es.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class ArticleFieldStatsResponse {

    private long total;
    private long took;
    private String field;
    private List<TermBucket> buckets;
}
