package com.zst.zsearch.es.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ArticleStatsRequest extends ArticleSearchRequest {

    private Integer topSize;
}
