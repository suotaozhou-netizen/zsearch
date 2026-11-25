package com.zst.zsearch.biz.search.parser;

import com.zst.zsearch.biz.model.dto.ArticleQueryConditionDto;
import com.zst.zsearch.biz.model.entity.MonitorSetting;
import com.zst.zsearch.common.enums.MonitorType;
import com.zst.zsearch.es.dto.ArticleSearchCondition;
import com.zst.zsearch.es.dto.ArticleSearchRequest;
import org.springframework.stereotype.Component;

@Component
public class GeneralQuerySearchParser extends AbstractMonitorSearchParser {

    @Override
    public MonitorType type() {
        return MonitorType.GENERAL_QUERY;
    }

    @Override
    public ArticleSearchRequest parse(ArticleQueryConditionDto dto, MonitorSetting setting) {
        if (setting != null) {
            throw new IllegalArgumentException("通用查询不支持 settingId");
        }
        ArticleSearchRequest request = buildRequest(dto);
        ArticleSearchCondition condition = pageCondition(dto);
        copyCondition(condition, request);
        return request;
    }
}
