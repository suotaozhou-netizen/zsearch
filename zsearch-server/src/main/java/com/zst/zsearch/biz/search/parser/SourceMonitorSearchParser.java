package com.zst.zsearch.biz.search.parser;

import com.zst.zsearch.biz.model.dto.ArticleQueryConditionDto;
import com.zst.zsearch.biz.model.entity.MonitorSetting;
import com.zst.zsearch.biz.model.entity.SourceMonitorSetting;
import com.zst.zsearch.biz.model.support.MonitorSettingValues;
import com.zst.zsearch.common.enums.MonitorType;
import com.zst.zsearch.es.dto.ArticleSearchCondition;
import com.zst.zsearch.es.dto.ArticleSearchRequest;
import com.zst.zsearch.es.query.EsSearchScope;
import com.zst.zsearch.es.query.FieldTermFilter;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class SourceMonitorSearchParser extends AbstractMonitorSearchParser {

    @Override
    public MonitorType type() {
        return MonitorType.SOURCE_MONITOR;
    }

    @Override
    public ArticleSearchRequest parse(ArticleQueryConditionDto dto, MonitorSetting setting) {
        throw new IllegalArgumentException("信息源监测设置不支持按关键词监测方案解析");
    }

    public ArticleSearchRequest parse(ArticleQueryConditionDto dto, SourceMonitorSetting setting) {
        if (setting == null) {
            throw new IllegalArgumentException("该监测类型必须提供 settingId");
        }

        ArticleSearchRequest request = buildRequest(dto);
        request.setKeywords(trimToNull(setting.getKeywords()));
        request.setExcludeKeywords(trimToNull(setting.getExcludeKeywords()));
        request.setSearchScope(EsSearchScope.FULL_TEXT.name());
        request.setFilters(buildFilters(setting));

        ArticleSearchCondition secondary = pageCondition(dto);
        if (hasCondition(secondary)) {
            request.setSecondary(secondary);
        }
        return request;
    }

    private List<FieldTermFilter> buildFilters(SourceMonitorSetting setting) {
        List<String> siteIds = MonitorSettingValues.split(setting.getSiteIds());
        if (siteIds.isEmpty()) {
            throw new IllegalStateException("信息源监测方案未配置平台: " + setting.getId());
        }

        List<FieldTermFilter> filters = new ArrayList<>(2);
        filters.add(new FieldTermFilter("siteId", siteIds));
        List<String> uids = MonitorSettingValues.split(setting.getUids());
        if (!uids.isEmpty()) {
            filters.add(new FieldTermFilter("userId", uids));
        }
        return filters;
    }
}