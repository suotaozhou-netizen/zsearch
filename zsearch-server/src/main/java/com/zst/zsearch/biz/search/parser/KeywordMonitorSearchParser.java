package com.zst.zsearch.biz.search.parser;

import com.zst.zsearch.biz.model.dto.ArticleQueryConditionDto;
import com.zst.zsearch.biz.model.entity.MonitorSetting;
import com.zst.zsearch.common.enums.MonitorType;
import com.zst.zsearch.es.dto.ArticleSearchCondition;
import com.zst.zsearch.es.dto.ArticleSearchRequest;
import com.zst.zsearch.es.query.EsSearchScope;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class KeywordMonitorSearchParser extends AbstractMonitorSearchParser {

    @Override
    public MonitorType type() {
        return MonitorType.KEYWORD_MONITOR;
    }

    @Override
    public ArticleSearchRequest parse(ArticleQueryConditionDto dto, MonitorSetting setting) {
        requireSetting(setting);
        if (!StringUtils.hasText(setting.getKeywords())) {
            throw new IllegalStateException("关键词监测方案缺少关键词: " + setting.getId());
        }

        ArticleSearchRequest request = buildRequest(dto);
        request.setKeywords(trimToNull(setting.getKeywords()));
        request.setExcludeKeywords(trimToNull(setting.getExcludeKeywords()));
        request.setSearchScope(EsSearchScope.FULL_TEXT.name());

        ArticleSearchCondition secondary = pageCondition(dto);
        if (hasCondition(secondary)) {
            request.setSecondary(secondary);
        }
        return request;
    }
}
