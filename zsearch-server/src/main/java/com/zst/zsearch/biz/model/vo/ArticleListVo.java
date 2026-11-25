package com.zst.zsearch.biz.model.vo;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class ArticleListVo {

    private long total;
    private int page;
    private int size;
    private long totalPages;
    private long took;
    private List<ArticleVo> articleList;
}
