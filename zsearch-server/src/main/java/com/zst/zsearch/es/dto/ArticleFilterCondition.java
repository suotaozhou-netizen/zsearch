package com.zst.zsearch.es.dto;

import com.zst.zsearch.es.query.FieldTermFilter;

import java.util.List;

public interface ArticleFilterCondition {

    String getKeywords();

    String getExcludeKeywords();

    Integer getNearSlop();

    String getSearchScope();

    String getStartTime();

    String getEndTime();

    List<String> getSources();

    List<String> getEmotions();

    List<String> getRiskLevels();

    List<FieldTermFilter> getFilters();
}
