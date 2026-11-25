package com.zst.zsearch.biz.search.parser;

import com.zst.zsearch.biz.model.dto.ArticleQueryConditionDto;
import com.zst.zsearch.biz.model.dto.MonitorSearchDto;
import com.zst.zsearch.biz.model.entity.MonitorSetting;
import com.zst.zsearch.common.enums.ArticleSortField;
import com.zst.zsearch.common.enums.SortOrder;
import com.zst.zsearch.es.dto.ArticleSearchCondition;
import com.zst.zsearch.es.dto.ArticleSearchRequest;
import com.zst.zsearch.es.query.EsSearchScope;
import com.zst.zsearch.es.query.SearchExpressionQueryBuilder;
import com.zst.zsearch.util.TimeRangePreset;
import com.zst.zsearch.util.TimeRangeResolver;
import com.zst.zsearch.util.TimeWindow;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

abstract class AbstractMonitorSearchParser implements MonitorSearchParser {

    private static final ZoneId ZONE = ZoneId.of("Asia/Shanghai");

    protected ArticleSearchRequest buildRequest(ArticleQueryConditionDto dto) {
        ArticleSearchRequest request = new ArticleSearchRequest();
        if (dto instanceof MonitorSearchDto searchDto) {
            request.setPage(searchDto.getPage());
            request.setSize(searchDto.getSize());

            ArticleSortField sortField = ArticleSortField.from(searchDto.getSortBy());
            SortOrder sortOrder = SortOrder.from(searchDto.getSortOrder());
            if (!sortField.supports(sortOrder)) {
                throw new IllegalArgumentException(
                        "排序字段 " + sortField.getValue() + " 仅支持降序");
            }
            request.setSortBy(sortField.getEsField());
            request.setSortOrder(sortOrder.getValue());
        }
        return request;
    }

    protected ArticleSearchCondition pageCondition(ArticleQueryConditionDto dto) {
        ArticleSearchCondition condition = new ArticleSearchCondition();
        condition.setKeywords(trimToNull(dto.getKeywords()));
        condition.setExcludeKeywords(trimToNull(dto.getExcludeKeywords()));
        condition.setNearSlop(validateNearSlop(dto.getNearSlop()));
        condition.setSearchScope(EsSearchScope.from(dto.getSearchScope()).name());
        applyTimeRange(condition, dto);
        condition.setSources(normalizeValues(dto.getSources()));
        condition.setEmotions(normalizeValues(dto.getEmotions()));
        condition.setRiskLevels(normalizeValues(dto.getRiskLevels()));
        return condition;
    }

    protected void copyCondition(ArticleSearchCondition source, ArticleSearchRequest target) {
        target.setKeywords(source.getKeywords());
        target.setExcludeKeywords(source.getExcludeKeywords());
        target.setNearSlop(source.getNearSlop());
        target.setSearchScope(source.getSearchScope());
        target.setStartTime(source.getStartTime());
        target.setEndTime(source.getEndTime());
        target.setSources(source.getSources());
        target.setEmotions(source.getEmotions());
        target.setRiskLevels(source.getRiskLevels());
    }

    protected boolean hasCondition(ArticleSearchCondition condition) {
        return StringUtils.hasText(condition.getKeywords())
                || StringUtils.hasText(condition.getExcludeKeywords())
                || condition.getStartTime() != null
                || condition.getEndTime() != null
                || hasValues(condition.getSources())
                || hasValues(condition.getEmotions())
                || hasValues(condition.getRiskLevels());
    }

    protected MonitorSetting requireSetting(MonitorSetting setting) {
        if (setting == null) {
            throw new IllegalArgumentException("该监测类型必须提供 settingId");
        }
        return setting;
    }

    protected String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private Integer validateNearSlop(Integer nearSlop) {
        if (nearSlop == null) {
            return null;
        }
        if (nearSlop < 0 || nearSlop > SearchExpressionQueryBuilder.MAX_NEAR_SLOP) {
            throw new IllegalArgumentException(
                    "nearSlop 必须在 0 到 " + SearchExpressionQueryBuilder.MAX_NEAR_SLOP + " 之间");
        }
        return nearSlop;
    }

    private void applyTimeRange(ArticleSearchCondition condition, ArticleQueryConditionDto dto) {
        if (!StringUtils.hasText(dto.getStartTime()) && !StringUtils.hasText(dto.getEndTime())) {
            return;
        }
        TimeWindow window = TimeRangeResolver.resolve(TimeRangePreset.CUSTOM,
                dto.getStartTime(), dto.getEndTime(), TimeRangeResolver.now());
        if (window.start() != null) {
            condition.setStartTime(formatTime(window.start()));
        }
        if (window.end() != null) {
            condition.setEndTime(formatTime(window.end()));
        }
    }

    private String formatTime(LocalDateTime time) {
        return time.atZone(ZONE).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
    }

    private List<String> normalizeValues(List<String> values) {
        if (values == null || values.isEmpty()) {
            return null;
        }
        List<String> normalized = values.stream()
                .filter(StringUtils::hasText)
                .map(String::trim)
                .distinct()
                .toList();
        return normalized.isEmpty() ? null : normalized;
    }

    private boolean hasValues(List<String> values) {
        return values != null && !values.isEmpty();
    }
}
