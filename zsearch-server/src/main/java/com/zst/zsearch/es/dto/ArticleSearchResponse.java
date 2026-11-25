package com.zst.zsearch.es.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class ArticleSearchResponse {
    private long total;
    private int page;
    private int size;
    private long totalPages;
    private long took;
    private List<Article> articleList;
}
