package com.zst.zsearch.es.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ArticleFieldStatsRequest extends ArticleSearchRequest {

    private String field;

    private Integer topSize;
}
