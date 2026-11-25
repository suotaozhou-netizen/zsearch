package com.zst.zsearch.biz.service;

import com.zst.zsearch.biz.model.dto.ArticleAuthorCountStatsDto;
import com.zst.zsearch.biz.model.dto.ArticleEmotionCountStatsDto;
import com.zst.zsearch.biz.model.dto.ArticleGroupByStatsDto;
import com.zst.zsearch.biz.model.dto.ArticleInteractionSumStatsDto;
import com.zst.zsearch.biz.model.dto.ArticleOverviewStatsDto;
import com.zst.zsearch.biz.model.dto.ArticleQueryConditionDto;
import com.zst.zsearch.biz.model.dto.ArticleSiteCountStatsDto;
import com.zst.zsearch.biz.model.dto.ArticleSiteTrendStatsDto;
import com.zst.zsearch.biz.model.dto.ArticleSumStatsDto;
import com.zst.zsearch.biz.model.dto.ArticleTrendStatsDto;
import com.zst.zsearch.biz.model.vo.ArticleEmotionSumVo;
import com.zst.zsearch.biz.model.vo.ArticleGroupByStatsVo;
import com.zst.zsearch.biz.model.vo.ArticleInteractionSumVo;
import com.zst.zsearch.biz.model.vo.ArticleOverviewStatsVo;
import com.zst.zsearch.biz.model.vo.ArticleSumStatsVo;
import com.zst.zsearch.biz.model.vo.ArticleTermCountVo;
import com.zst.zsearch.biz.model.vo.ArticleTrendBucketVo;
import com.zst.zsearch.biz.model.vo.ArticleTrendStatsVo;
import com.zst.zsearch.biz.search.parser.MonitorSearchParserRegistry;
import com.zst.zsearch.common.enums.Emotion;
import com.zst.zsearch.common.enums.MonitorType;
import com.zst.zsearch.common.enums.SiteId;
import com.zst.zsearch.es.dto.ArticleFieldStatsRequest;
import com.zst.zsearch.es.dto.ArticleFieldStatsResponse;
import com.zst.zsearch.es.dto.ArticleSearchRequest;
import com.zst.zsearch.es.dto.ArticleStatsRequest;
import com.zst.zsearch.es.dto.ArticleStatsResponse;
import com.zst.zsearch.es.dto.ArticleSumStatsResponse;
import com.zst.zsearch.es.dto.ArticleTrendStatsRequest;
import com.zst.zsearch.es.dto.ArticleTrendStatsResponse;
import com.zst.zsearch.es.dto.TermBucket;
import com.zst.zsearch.es.service.ArticleStatsService;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class ArticleStatsQueryService {

    private final MonitorSearchParserRegistry parserRegistry;
    private final ArticleStatsService esStatsService;

    public ArticleStatsQueryService(MonitorSearchParserRegistry parserRegistry,
                                    ArticleStatsService esStatsService) {
        this.parserRegistry = parserRegistry;
        this.esStatsService = esStatsService;
    }

    public ArticleOverviewStatsVo overview(ArticleOverviewStatsDto dto) {
        ArticleStatsRequest request = copyConditions(parse(dto), new ArticleStatsRequest());
        request.setTopSize(dto.getTopSize());
        return toOverviewVo(esStatsService.stats(request));
    }

    public ArticleGroupByStatsVo groupBy(ArticleGroupByStatsDto dto) {
        ArticleFieldStatsRequest request = copyConditions(parse(dto), new ArticleFieldStatsRequest());
        request.setField(dto.getField());
        request.setTopSize(dto.getTopSize());
        return toGroupByVo(esStatsService.fieldStats(request));
    }

    public Map<String, Long> countGroupBySiteId(ArticleSiteCountStatsDto dto) {
        return toSiteCountMap(esStatsService.countGroupBySiteId(parse(dto)));
    }

    public Map<String, Long> countTopAuthors(ArticleAuthorCountStatsDto dto) {
        return toAuthorCountMap(esStatsService.countTopAuthors(parse(dto)));
    }

    public Map<String, Long> countGroupByEmotion(ArticleEmotionCountStatsDto dto) {
        return toEmotionCountMap(esStatsService.countGroupByEmotion(parse(dto)));
    }

    public Map<String, Map<String, Long>> siteTrend(ArticleSiteTrendStatsDto dto) {
        ArticleTrendStatsRequest request = copyConditions(parse(dto), new ArticleTrendStatsRequest());
        request.setInterval(dto.getInterval());
        return toSiteTrendMap(esStatsService.siteTrendStats(request));
    }

    public long interactionSum(ArticleInteractionSumStatsDto dto) {
        return esStatsService.interactionSum(parse(dto));
    }

    public ArticleSumStatsVo sum(ArticleSumStatsDto dto) {
        return toSumVo(esStatsService.sumStats(parse(dto)));
    }

    public ArticleTrendStatsVo trend(ArticleTrendStatsDto dto) {
        ArticleTrendStatsRequest request = copyConditions(parse(dto), new ArticleTrendStatsRequest());
        request.setTimeField(dto.getTimeField());
        request.setInterval(dto.getInterval());
        return toTrendVo(esStatsService.trendStats(request));
    }

    private ArticleSearchRequest parse(ArticleQueryConditionDto dto) {
        if (dto.getMonitorType() == null) {
            throw new IllegalArgumentException("监测类型不能为空");
        }
        MonitorType type = MonitorType.fromCode(dto.getMonitorType());
        return parserRegistry.get(type).parse(dto, null);
    }

    private <T extends ArticleSearchRequest> T copyConditions(ArticleSearchRequest source, T target) {
        target.setKeywords(source.getKeywords());
        target.setExcludeKeywords(source.getExcludeKeywords());
        target.setNearSlop(source.getNearSlop());
        target.setSearchScope(source.getSearchScope());
        target.setStartTime(source.getStartTime());
        target.setEndTime(source.getEndTime());
        target.setSources(source.getSources());
        target.setEmotions(source.getEmotions());
        target.setRiskLevels(source.getRiskLevels());
        target.setFilters(source.getFilters());
        target.setSecondary(source.getSecondary());
        return target;
    }

    private ArticleOverviewStatsVo toOverviewVo(ArticleStatsResponse response) {
        ArticleOverviewStatsVo result = new ArticleOverviewStatsVo();
        result.setTotal(response.getTotal());
        result.setTook(response.getTook());
        result.setSources(toTermCountVos(response.getSources()));
        result.setEmotions(toTermCountVos(response.getEmotions()));
        result.setRiskLevels(toTermCountVos(response.getRiskLevels()));
        return result;
    }

    private ArticleGroupByStatsVo toGroupByVo(ArticleFieldStatsResponse response) {
        ArticleGroupByStatsVo result = new ArticleGroupByStatsVo();
        result.setTotal(response.getTotal());
        result.setTook(response.getTook());
        result.setField(response.getField());
        result.setBuckets(toTermCountVos(response.getBuckets()));
        return result;
    }

    private Map<String, Long> toSiteCountMap(ArticleFieldStatsResponse response) {
        Map<String, Long> counts = new HashMap<>();
        if (response.getBuckets() != null) {
            for (TermBucket bucket : response.getBuckets()) {
                counts.put(bucket.getKey(), bucket.getCount());
            }
        }

        Map<String, Long> siteCounts = new LinkedHashMap<>();
        long total = 0;
        for (SiteId siteId : SiteId.values()) {
            long count = counts.getOrDefault(String.valueOf(siteId.getCode()), 0L);
            siteCounts.put(String.valueOf(siteId.getCode()), count);
            total += count;
        }

        Map<String, Long> result = new LinkedHashMap<>();
        result.put("total", total);
        result.putAll(siteCounts);
        return result;
    }

    private Map<String, Long> toAuthorCountMap(ArticleFieldStatsResponse response) {
        Map<String, Long> result = new LinkedHashMap<>();
        long total = 0;
        if (response.getBuckets() != null) {
            for (TermBucket term : response.getBuckets()) {
                result.put(term.getKey(), term.getCount());
                total += term.getCount();
            }
        }
        result.put("total", total);
        return result;
    }

    private Map<String, Long> toEmotionCountMap(ArticleFieldStatsResponse response) {
        Map<String, Long> counts = new HashMap<>();
        if (response.getBuckets() != null) {
            for (TermBucket term : response.getBuckets()) {
                counts.put(term.getKey(), term.getCount());
            }
        }

        Map<String, Long> result = new LinkedHashMap<>();
        long total = 0;
        for (Emotion emotion : Emotion.values()) {
            long count = counts.getOrDefault(emotion.getLabel(), 0L);
            result.put(emotion.getLabel(), count);
            total += count;
        }
        result.put("total", total);
        return result;
    }

    private Map<String, Map<String, Long>> toSiteTrendMap(ArticleTrendStatsResponse response) {
        Map<String, Map<String, Long>> result = new LinkedHashMap<>();
        if (response.getBuckets() == null) {
            return result;
        }

        for (ArticleTrendStatsResponse.TrendBucket bucket : response.getBuckets()) {
            Map<String, Long> counts = new HashMap<>();
            if (bucket.getSiteCounts() != null) {
                for (TermBucket term : bucket.getSiteCounts()) {
                    counts.put(term.getKey(), term.getCount());
                }
            }

            Map<String, Long> row = new LinkedHashMap<>();
            long total = 0;
            for (SiteId siteId : SiteId.values()) {
                long count = counts.getOrDefault(String.valueOf(siteId.getCode()), 0L);
                row.put(String.valueOf(siteId.getCode()), count);
                total += count;
            }
            row.put("total", total);

            String time = bucket.getKeyAsString() == null ? bucket.getKey() : bucket.getKeyAsString();
            result.put(time, row);
        }
        return result;
    }

    private ArticleSumStatsVo toSumVo(ArticleSumStatsResponse response) {
        ArticleSumStatsVo result = new ArticleSumStatsVo();
        result.setTotal(response.getTotal());
        result.setTook(response.getTook());
        result.setSums(toInteractionSumVo(response.getSums()));
        result.setByEmotion(response.getByEmotion() == null
                ? List.of()
                : response.getByEmotion().stream().map(this::toEmotionSumVo).toList());
        return result;
    }

    private ArticleEmotionSumVo toEmotionSumVo(ArticleSumStatsResponse.EmotionSumBucket bucket) {
        ArticleEmotionSumVo result = new ArticleEmotionSumVo();
        result.setEmotion(bucket.getKey());
        result.setCount(bucket.getCount());
        result.setSums(toInteractionSumVo(bucket.getSums()));
        return result;
    }

    private ArticleInteractionSumVo toInteractionSumVo(ArticleSumStatsResponse.Sums sums) {
        ArticleInteractionSumVo result = new ArticleInteractionSumVo();
        if (sums == null) {
            return result;
        }
        result.setRepostCount(sums.getRepostCount());
        result.setCommentCount(sums.getCommentCount());
        result.setLikeCount(sums.getLikeCount());
        result.setFollowerCount(sums.getFollowerCount());
        result.setCollectCount(sums.getCollectCount());
        return result;
    }

    private ArticleTrendStatsVo toTrendVo(ArticleTrendStatsResponse response) {
        ArticleTrendStatsVo result = new ArticleTrendStatsVo();
        result.setTotal(response.getTotal());
        result.setTook(response.getTook());
        result.setTimeField(response.getTimeField());
        result.setInterval(response.getInterval());
        result.setBuckets(response.getBuckets() == null
                ? List.of()
                : response.getBuckets().stream().map(this::toTrendBucketVo).toList());
        return result;
    }

    private ArticleTrendBucketVo toTrendBucketVo(ArticleTrendStatsResponse.TrendBucket bucket) {
        ArticleTrendBucketVo result = new ArticleTrendBucketVo();
        result.setKey(bucket.getKey());
        result.setKeyAsString(bucket.getKeyAsString());
        result.setCount(bucket.getCount());
        result.setInteractionCount(bucket.getInteractionCount());
        result.setSiteCounts(toTermCountVos(bucket.getSiteCounts()));
        return result;
    }

    private List<ArticleTermCountVo> toTermCountVos(List<TermBucket> buckets) {
        if (buckets == null || buckets.isEmpty()) {
            return List.of();
        }
        return buckets.stream().map(this::toTermCountVo).toList();
    }

    private ArticleTermCountVo toTermCountVo(TermBucket bucket) {
        ArticleTermCountVo result = new ArticleTermCountVo();
        result.setKey(bucket.getKey());
        result.setCount(bucket.getCount());
        return result;
    }
}
