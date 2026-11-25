package com.zst.zsearch.biz.search.export;

import com.zst.zsearch.biz.model.dto.ArticleQueryConditionDto;
import com.zst.zsearch.biz.search.parser.MonitorSearchParserRegistry;
import com.zst.zsearch.biz.search.parser.SourceMonitorSearchParser;
import com.zst.zsearch.biz.service.MonitorService;
import com.zst.zsearch.biz.service.SourceMonitorSettingService;
import com.zst.zsearch.common.enums.MonitorType;
import com.zst.zsearch.es.dto.ArticleSearchRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

/**
 * 复用页面检索的监测类型解析逻辑，将导出条件转换为 ES 查询条件
 */
@Component
public class ArticleExportConditionResolver {

    private final MonitorSearchParserRegistry parserRegistry;
    private final SourceMonitorSearchParser sourceMonitorSearchParser;
    private final MonitorService monitorService;
    private final SourceMonitorSettingService sourceMonitorSettingService;

    public ArticleExportConditionResolver(MonitorSearchParserRegistry parserRegistry,
                                          SourceMonitorSearchParser sourceMonitorSearchParser,
                                          MonitorService monitorService,
                                          SourceMonitorSettingService sourceMonitorSettingService) {
        this.parserRegistry = parserRegistry;
        this.sourceMonitorSearchParser = sourceMonitorSearchParser;
        this.monitorService = monitorService;
        this.sourceMonitorSettingService = sourceMonitorSettingService;
    }

    public ArticleSearchRequest resolve(ArticleQueryConditionDto dto) {
        if (dto.getMonitorType() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "监测类型不能为空");
        }
        MonitorType type;
        try {
            type = MonitorType.fromCode(dto.getMonitorType());
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage(), e);
        }
        try {
            return switch (type) {
                case GENERAL_QUERY -> {
                    if (dto.getSettingId() != null) {
                        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "通用查询不需要 settingId");
                    }
                    yield parserRegistry.get(type).parse(dto, null);
                }
                case KEYWORD_MONITOR -> parserRegistry.get(type)
                        .parse(dto, monitorService.loadActiveSetting(requireSettingId(dto)));
                case SOURCE_MONITOR -> sourceMonitorSearchParser
                        .parse(dto, sourceMonitorSettingService.loadActiveSetting(requireSettingId(dto)));
            };
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage(), e);
        }
    }

    private Long requireSettingId(ArticleQueryConditionDto dto) {
        if (dto.getSettingId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "监测类型必须提供 settingId");
        }
        return dto.getSettingId();
    }
}