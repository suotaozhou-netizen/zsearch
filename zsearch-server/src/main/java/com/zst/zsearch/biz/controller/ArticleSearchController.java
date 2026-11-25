package com.zst.zsearch.biz.controller;

import com.zst.zsearch.biz.model.dto.ArticleGroupByStatsDto;
import com.zst.zsearch.biz.model.dto.ArticleOverviewStatsDto;
import com.zst.zsearch.biz.model.dto.ArticleAuthorCountStatsDto;
import com.zst.zsearch.biz.model.dto.ArticleEmotionCountStatsDto;
import com.zst.zsearch.biz.model.dto.ArticleInteractionSumStatsDto;
import com.zst.zsearch.biz.model.dto.ArticleQueryConditionDto;
import com.zst.zsearch.biz.model.dto.ArticleSiteTrendStatsDto;
import com.zst.zsearch.biz.model.dto.ArticleSiteCountStatsDto;
import com.zst.zsearch.biz.model.dto.ArticleSumStatsDto;
import com.zst.zsearch.biz.model.dto.ArticleTrendStatsDto;
import com.zst.zsearch.biz.model.dto.MonitorSearchDto;
import com.zst.zsearch.biz.model.entity.MonitorSetting;
import com.zst.zsearch.biz.model.entity.SourceMonitorSetting;
import com.zst.zsearch.biz.model.vo.ArticleGroupByStatsVo;
import com.zst.zsearch.biz.model.vo.ArticleListVo;
import com.zst.zsearch.biz.model.vo.ArticleOverviewStatsVo;
import com.zst.zsearch.biz.model.vo.ArticleSumStatsVo;
import com.zst.zsearch.biz.model.vo.ArticleTrendStatsVo;
import com.zst.zsearch.biz.model.vo.ArticleVo;
import com.zst.zsearch.biz.search.enricher.ArticleVoEnrichContext;
import com.zst.zsearch.biz.search.parser.MonitorSearchParserRegistry;
import com.zst.zsearch.biz.search.parser.SourceMonitorSearchParser;
import com.zst.zsearch.biz.service.ArticleStatsQueryService;
import com.zst.zsearch.biz.service.ArticleVoAssembler;
import com.zst.zsearch.biz.service.MonitorService;
import com.zst.zsearch.biz.service.SourceMonitorSettingService;
import com.zst.zsearch.common.ApiResponse;
import com.zst.zsearch.common.enums.MonitorType;
import com.zst.zsearch.es.dto.Article;
import com.zst.zsearch.es.dto.ArticleSearchRequest;
import com.zst.zsearch.es.dto.ArticleSearchResponse;
import com.zst.zsearch.es.service.ArticleSearchService;
import com.zst.zsearch.es.support.EsQueryLogSupport;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/search")
public class ArticleSearchController {

    private final MonitorSearchParserRegistry parserRegistry;
    private final SourceMonitorSearchParser sourceMonitorSearchParser;
    private final ArticleSearchService articleSearchService;
    private final ArticleVoAssembler articleVoAssembler;
    private final ArticleStatsQueryService articleStatsQueryService;
    private final MonitorService monitorService;
    private final SourceMonitorSettingService sourceMonitorSettingService;

    public ArticleSearchController(MonitorSearchParserRegistry parserRegistry,
                                   SourceMonitorSearchParser sourceMonitorSearchParser,
                                   ArticleSearchService articleSearchService,
                                   ArticleVoAssembler articleVoAssembler,
                                   ArticleStatsQueryService articleStatsQueryService,
                                   MonitorService monitorService,
                                   SourceMonitorSettingService sourceMonitorSettingService) {
        this.parserRegistry = parserRegistry;
        this.sourceMonitorSearchParser = sourceMonitorSearchParser;
        this.articleSearchService = articleSearchService;
        this.articleVoAssembler = articleVoAssembler;
        this.articleStatsQueryService = articleStatsQueryService;
        this.monitorService = monitorService;
        this.sourceMonitorSettingService = sourceMonitorSettingService;
    }

    @PostMapping
    public ApiResponse<ArticleListVo> search(@Valid @RequestBody MonitorSearchDto request) {
        log.info("检索接口参数: {}", EsQueryLogSupport.json(request));
        MonitorType type = MonitorType.fromCode(request.getMonitorType());
        ArticleSearchRequest searchRequest;
        String settingName = null;

        switch (type) {
            case GENERAL_QUERY -> {
                requireNoSettingId(request);
                searchRequest = parserRegistry.get(type).parse(request, null);
            }
            case KEYWORD_MONITOR -> {
                MonitorSetting setting = monitorService.loadActiveSetting(requireSettingId(request));
                searchRequest = parserRegistry.get(type).parse(request, setting);
                settingName = setting.getName();
            }
            case SOURCE_MONITOR -> {
                SourceMonitorSetting setting = sourceMonitorSettingService
                        .loadActiveSetting(requireSettingId(request));
                searchRequest = sourceMonitorSearchParser.parse(request, setting);
                settingName = setting.getName();
            }
            default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "不支持的监测类型: " + type);
        }

        ArticleSearchResponse response = articleSearchService.search(searchRequest);
        String keywordExpression = searchRequest.getKeywords();
        List<String> keywords = keywordExpression == null || keywordExpression.isBlank()
                ? List.of()
                : List.of(keywordExpression);
        return ApiResponse.success(articleVoAssembler.assemble(response,
                new ArticleVoEnrichContext(request.getSettingId(), settingName, null, keywords)));
    }

    @GetMapping("/{id}")
    public ApiResponse<ArticleVo> getArticle(@PathVariable String id) {
        Article article = articleSearchService.getById(id);
        return ApiResponse.success(articleVoAssembler.assemble(article));
    }

    @PostMapping("/overview")
    public ApiResponse<ArticleOverviewStatsVo> overview(
            @Valid @RequestBody ArticleOverviewStatsDto request) {
        log.info("统计接口参数[概览]: {}", EsQueryLogSupport.json(request));
        requireGeneralQuery(request);
        return ApiResponse.success(articleStatsQueryService.overview(request));
    }

    @PostMapping("/group-by")
    public ApiResponse<ArticleGroupByStatsVo> groupBy(
            @Valid @RequestBody ArticleGroupByStatsDto request) {
        log.info("统计接口参数[分组]: {}", EsQueryLogSupport.json(request));
        requireGeneralQuery(request);
        return ApiResponse.success(articleStatsQueryService.groupBy(request));
    }

    @PostMapping("/site-count")
    public ApiResponse<Map<String, Long>> countGroupBySiteId(
            @Valid @RequestBody ArticleSiteCountStatsDto request) {
        log.info("统计接口参数[站点类型]: {}", EsQueryLogSupport.json(request));
        requireGeneralQuery(request);
        return ApiResponse.success(articleStatsQueryService.countGroupBySiteId(request));
    }

    @PostMapping("/author-count")
    public ApiResponse<Map<String, Long>> countTopAuthors(
            @Valid @RequestBody ArticleAuthorCountStatsDto request) {
        log.info("统计接口参数[作者]: {}", EsQueryLogSupport.json(request));
        requireGeneralQuery(request);
        return ApiResponse.success(articleStatsQueryService.countTopAuthors(request));
    }

    @PostMapping("/emotion-count")
    public ApiResponse<Map<String, Long>> countGroupByEmotion(
            @Valid @RequestBody ArticleEmotionCountStatsDto request) {
        log.info("统计接口参数[情感类型]: {}", EsQueryLogSupport.json(request));
        requireGeneralQuery(request);
        return ApiResponse.success(articleStatsQueryService.countGroupByEmotion(request));
    }

    @PostMapping("/site-trend")
    public ApiResponse<Map<String, Map<String, Long>>> siteTrend(
            @Valid @RequestBody ArticleSiteTrendStatsDto request) {
        log.info("统计接口参数[站点时间趋势]: {}", EsQueryLogSupport.json(request));
        requireGeneralQuery(request);
        return ApiResponse.success(articleStatsQueryService.siteTrend(request));
    }

    @PostMapping("/interaction-sum")
    public ApiResponse<Long> interactionSum(
            @Valid @RequestBody ArticleInteractionSumStatsDto request) {
        log.info("统计接口参数[互动量汇总]: {}", EsQueryLogSupport.json(request));
        requireGeneralQuery(request);
        return ApiResponse.success(articleStatsQueryService.interactionSum(request));
    }

    @PostMapping("/sum")
    public ApiResponse<ArticleSumStatsVo> sum(
            @Valid @RequestBody ArticleSumStatsDto request) {
        log.info("统计接口参数[汇总]: {}", EsQueryLogSupport.json(request));
        requireGeneralQuery(request);
        return ApiResponse.success(articleStatsQueryService.sum(request));
    }

    @PostMapping("/trend")
    public ApiResponse<ArticleTrendStatsVo> trend(
            @Valid @RequestBody ArticleTrendStatsDto request) {
        log.info("统计接口参数[趋势]: {}", EsQueryLogSupport.json(request));
        requireGeneralQuery(request);
        return ApiResponse.success(articleStatsQueryService.trend(request));
    }

    private Long requireSettingId(MonitorSearchDto request) {
        if (request.getSettingId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "监测类型必须提供 settingId");
        }
        return request.getSettingId();
    }

    private void requireNoSettingId(MonitorSearchDto request) {
        if (request.getSettingId() != null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "通用查询不需要 settingId");
        }
    }

    private void requireGeneralQuery(ArticleQueryConditionDto request) {
        if (request.getMonitorType() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "监测类型不能为空");
        }
        MonitorType type = MonitorType.fromCode(request.getMonitorType());
        if (type != MonitorType.GENERAL_QUERY) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "当前接口仅支持 monitorType=" + MonitorType.GENERAL_QUERY.getCode());
        }
        if (request.getSettingId() != null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "当前接口不需要 settingId");
        }
    }
}