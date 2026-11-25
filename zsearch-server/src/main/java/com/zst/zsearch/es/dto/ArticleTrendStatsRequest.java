package com.zst.zsearch.es.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ArticleTrendStatsRequest extends ArticleSearchRequest {

    private String timeField;

    private String interval;
}
