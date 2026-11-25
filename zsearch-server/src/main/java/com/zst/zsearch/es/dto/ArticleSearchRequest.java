package com.zst.zsearch.es.dto;

import com.zst.zsearch.es.query.FieldTermFilter;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class ArticleSearchRequest implements ArticleFilterCondition {

    private String keywords;

    private String excludeKeywords;

    private Integer nearSlop;

    private String searchScope;

    private String startTime;

    private String endTime;

    private List<String> sources;

    private List<String> emotions;

    private List<String> riskLevels;

    private List<FieldTermFilter> filters;

    // 二次检索条件，与主条件同时满足
    private ArticleSearchCondition secondary;

    private Integer page;

    private Integer size;

    private String sortBy;

    private String sortOrder;
}
