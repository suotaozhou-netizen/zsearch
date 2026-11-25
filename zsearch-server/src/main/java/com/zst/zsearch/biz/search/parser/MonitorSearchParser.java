package com.zst.zsearch.biz.search.parser;

import com.zst.zsearch.biz.model.dto.ArticleQueryConditionDto;
import com.zst.zsearch.biz.model.entity.MonitorSetting;
import com.zst.zsearch.common.enums.MonitorType;
import com.zst.zsearch.es.dto.ArticleSearchRequest;

public interface MonitorSearchParser {

    MonitorType type();

    ArticleSearchRequest parse(ArticleQueryConditionDto dto, MonitorSetting setting);
}
