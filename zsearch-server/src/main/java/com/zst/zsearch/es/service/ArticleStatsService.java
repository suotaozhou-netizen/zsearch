package com.zst.zsearch.es.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.ElasticsearchException;
import co.elastic.clients.elasticsearch._types.aggregations.Aggregate;
import co.elastic.clients.elasticsearch._types.aggregations.CalendarInterval;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import com.zst.zsearch.es.config.EsProperties;
import com.zst.zsearch.es.dto.ArticleFieldStatsRequest;
import com.zst.zsearch.es.dto.ArticleFieldStatsResponse;
import com.zst.zsearch.es.dto.ArticleSearchRequest;
import com.zst.zsearch.es.dto.ArticleStatsRequest;
import com.zst.zsearch.es.dto.ArticleStatsResponse;
import com.zst.zsearch.es.dto.ArticleSumStatsResponse;
import com.zst.zsearch.es.dto.ArticleTrendStatsRequest;
import com.zst.zsearch.es.dto.ArticleTrendStatsResponse;
import com.zst.zsearch.es.dto.TermBucket;
import com.zst.zsearch.es.support.EsQueryLogSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class ArticleStatsService {

    private static final Logger log = LoggerFactory.getLogger(ArticleStatsService.class);

    private static final int DEFAULT_TOP_SIZE = 20;
    private static final int MAX_TOP_SIZE = 100;

    private static final String AGG_SOURCES = "by_source";
    private static final String AGG_EMOTIONS = "by_emotion";
    private static final String AGG_RISK_LEVELS = "by_risk_level";
    private static final String AGG_FIELD = "by_field";
    private static final String AGG_SITE_COUNTS = "by_site";
    private static final String AGG_AUTHORS = "by_author";
    private static final int AUTHOR_BUCKET_SIZE = 10;
    private static final String AGG_EMOTION_SUMS = "by_emotion_sums";
    private static final int EMOTION_BUCKET_SIZE = 20;

    private static final String SUM_REPOST = "sum_repost_count";
    private static final String SUM_COMMENT = "sum_comment_count";
    private static final String SUM_LIKE = "sum_like_count";
    private static final String SUM_FOLLOWER = "sum_follower_count";
    private static final String SUM_COLLECT = "sum_collect_count";

    private static final String AGG_TREND = "by_time";
    private static final String AGG_TREND_SITES = "by_site";
    private static final String SUM_INTERACTION = "sum_interaction_count";
    private static final int SITE_BUCKET_SIZE = 20;
    private static final String SITE_TIME_ZONE = "+08:00";
    private static final String SITE_TIME_FORMAT = "yyyy-MM-dd HH:mm:ss";

    private static final Map<String, TimeFieldSpec> TIME_FIELDS = Map.of(
            "createdat", new TimeFieldSpec("createdAt", "created_at"),
            "crawltime", new TimeFieldSpec("crawlerTime", "crawler_time")
    );
    private static final Map<String, CalendarInterval> INTERVALS = Map.of(
            "hour", CalendarInterval.Hour,
            "day", CalendarInterval.Day,
            "week", CalendarInterval.Week,
            "month", CalendarInterval.Month
    );

    private record TimeFieldSpec(String apiName, String esField) {
    }

    private static final String SUPPORTED_FIELDS = "name, source, siteId, userId, emotion, riskLevel";
    private static final Map<String, FieldSpec> FIELD_SPECS = Map.of(
            "name", new FieldSpec("name", "name"),
            "source", new FieldSpec("source", "source"),
            "siteid", new FieldSpec("siteId", "siteId"),
            "userid", new FieldSpec("userId", "userId"),
            "emotion", new FieldSpec("emotion", "emotion"),
            "risklevel", new FieldSpec("riskLevel", "risk_level")
    );

    private record FieldSpec(String apiName, String esField) {
    }

    private final ElasticsearchClient client;
    private final EsProperties properties;

    public ArticleStatsService(ElasticsearchClient client, EsProperties properties) {
        this.client = client;
        this.properties = properties;
    }

    public ArticleStatsResponse stats(ArticleStatsRequest request) {
        int topSize = resolveTopSize(request.getTopSize());
        log.info("ES 统计参数[概览]: index={}, topSize={}, request={}",
                properties.getIndexName(), topSize, EsQueryLogSupport.json(request));

        BoolQuery query = ArticleQueryFactory.build(request);

        SearchRequest search = new SearchRequest.Builder()
                .index(properties.getIndexName())
                .query(q -> q.bool(query))
                .size(0)
                .trackTotalHits(t -> t.enabled(true))
                .aggregations(AGG_SOURCES, a -> a.terms(t -> t.field("source").size(topSize)))
                .aggregations(AGG_EMOTIONS, a -> a.terms(t -> t.field("emotion").size(topSize)))
                .aggregations(AGG_RISK_LEVELS, a -> a.terms(t -> t.field("risk_level").size(topSize)))
                .build();

        return toResponse(execute(search, "stats/overview"));
    }

    public ArticleFieldStatsResponse fieldStats(ArticleFieldStatsRequest request) {
        FieldSpec field = resolveField(request.getField());
        int topSize = resolveTopSize(request.getTopSize());
        log.info("ES 统计参数[分组]: index={}, field={}, topSize={}, request={}",
                properties.getIndexName(), field.apiName(), topSize, EsQueryLogSupport.json(request));

        BoolQuery query = ArticleQueryFactory.build(request);

        SearchRequest search = new SearchRequest.Builder()
                .index(properties.getIndexName())
                .query(q -> q.bool(query))
                .size(0)
                .trackTotalHits(t -> t.enabled(true))
                .aggregations(AGG_FIELD, a -> a.terms(t -> t.field(field.esField()).size(topSize)))
                .build();

        SearchResponse<Void> response = execute(search, "stats/group-by");
        ArticleFieldStatsResponse result = new ArticleFieldStatsResponse();
        result.setTotal(response.hits().total() == null ? 0 : response.hits().total().value());
        result.setTook(response.took());
        result.setField(field.apiName());
        result.setBuckets(readBuckets(response.aggregations().get(AGG_FIELD)));
        return result;
    }

    public ArticleFieldStatsResponse countGroupBySiteId(ArticleSearchRequest request) {
        log.info("ES 统计参数[站点类型]: index={}, request={}",
                properties.getIndexName(), EsQueryLogSupport.json(request));

        BoolQuery query = ArticleQueryFactory.build(request);

        SearchRequest search = new SearchRequest.Builder()
                .index(properties.getIndexName())
                .query(q -> q.bool(query))
                .size(0)
                .trackTotalHits(t -> t.enabled(true))
                .aggregations(AGG_SITE_COUNTS, a -> a.terms(t -> t.field("siteId").size(SITE_BUCKET_SIZE)))
                .build();

        SearchResponse<Void> response = execute(search, "stats/site-count");
        ArticleFieldStatsResponse result = new ArticleFieldStatsResponse();
        result.setTotal(response.hits().total() == null ? 0 : response.hits().total().value());
        result.setTook(response.took());
        result.setField("siteId");
        result.setBuckets(readBuckets(response.aggregations().get(AGG_SITE_COUNTS)));
        return result;
    }

    public ArticleFieldStatsResponse countGroupByEmotion(ArticleSearchRequest request) {
        log.info("ES 统计参数[情感类型]: index={}, request={}",
                properties.getIndexName(), EsQueryLogSupport.json(request));

        BoolQuery query = ArticleQueryFactory.build(request);

        SearchRequest search = new SearchRequest.Builder()
                .index(properties.getIndexName())
                .query(q -> q.bool(query))
                .size(0)
                .trackTotalHits(t -> t.enabled(true))
                .aggregations(AGG_EMOTIONS, a -> a.terms(t -> t.field("emotion").size(EMOTION_BUCKET_SIZE)))
                .build();

        SearchResponse<Void> response = execute(search, "stats/emotion-count");
        ArticleFieldStatsResponse result = new ArticleFieldStatsResponse();
        result.setTotal(response.hits().total() == null ? 0 : response.hits().total().value());
        result.setTook(response.took());
        result.setField("emotion");
        result.setBuckets(readBuckets(response.aggregations().get(AGG_EMOTIONS)));
        return result;
    }

    public ArticleFieldStatsResponse countTopAuthors(ArticleSearchRequest request) {
        log.info("ES 统计参数[作者]: index={}, topSize={}, request={}",
                properties.getIndexName(), AUTHOR_BUCKET_SIZE, EsQueryLogSupport.json(request));

        BoolQuery query = ArticleQueryFactory.build(request);

        SearchRequest search = new SearchRequest.Builder()
                .index(properties.getIndexName())
                .query(q -> q.bool(query))
                .size(0)
                .trackTotalHits(t -> t.enabled(true))
                .aggregations(AGG_AUTHORS, a -> a.terms(t -> t.field("name").size(AUTHOR_BUCKET_SIZE)))
                .build();

        SearchResponse<Void> response = execute(search, "stats/author-count");
        ArticleFieldStatsResponse result = new ArticleFieldStatsResponse();
        result.setTotal(response.hits().total() == null ? 0 : response.hits().total().value());
        result.setTook(response.took());
        result.setField("name");
        result.setBuckets(readBuckets(response.aggregations().get(AGG_AUTHORS)));
        return result;
    }

    public ArticleSumStatsResponse sumStats(ArticleSearchRequest request) {
        log.info("ES 统计参数[汇总]: index={}, request={}",
                properties.getIndexName(), EsQueryLogSupport.json(request));

        BoolQuery query = ArticleQueryFactory.build(request);

        SearchRequest search = new SearchRequest.Builder()
                .index(properties.getIndexName())
                .query(q -> q.bool(query))
                .size(0)
                .trackTotalHits(t -> t.enabled(true))
                .aggregations(SUM_REPOST, a -> a.sum(s -> s.field("repost_count")))
                .aggregations(SUM_COMMENT, a -> a.sum(s -> s.field("comment_count")))
                .aggregations(SUM_LIKE, a -> a.sum(s -> s.field("like_count")))
                .aggregations(SUM_FOLLOWER, a -> a.sum(s -> s.field("follower_count")))
                .aggregations(SUM_COLLECT, a -> a.sum(s -> s.field("collect_count")))
                .aggregations(AGG_EMOTION_SUMS, a -> a
                        .terms(t -> t.field("emotion").size(EMOTION_BUCKET_SIZE))
                        .aggregations(SUM_REPOST, sa -> sa.sum(s -> s.field("repost_count")))
                        .aggregations(SUM_COMMENT, sa -> sa.sum(s -> s.field("comment_count")))
                        .aggregations(SUM_LIKE, sa -> sa.sum(s -> s.field("like_count")))
                        .aggregations(SUM_FOLLOWER, sa -> sa.sum(s -> s.field("follower_count")))
                        .aggregations(SUM_COLLECT, sa -> sa.sum(s -> s.field("collect_count"))))
                .build();

        return toSumResponse(execute(search, "stats/sum"));
    }

    public long interactionSum(ArticleSearchRequest request) {
        log.info("ES 统计参数[互动量汇总]: index={}, request={}",
                properties.getIndexName(), EsQueryLogSupport.json(request));

        BoolQuery query = ArticleQueryFactory.build(request);

        SearchRequest search = new SearchRequest.Builder()
                .index(properties.getIndexName())
                .query(q -> q.bool(query))
                .size(0)
                .trackTotalHits(t -> t.enabled(true))
                .aggregations(SUM_INTERACTION, a -> a.sum(s -> s.field("interaction_count")))
                .build();

        SearchResponse<Void> response = execute(search, "stats/interaction-sum");
        return readSum(response.aggregations(), SUM_INTERACTION);
    }

    public ArticleTrendStatsResponse siteTrendStats(ArticleTrendStatsRequest request) {
        CalendarInterval interval = resolveSiteTrendInterval(request.getInterval());
        log.info("ES 统计参数[站点时间趋势]: index={}, interval={}, request={}",
                properties.getIndexName(), interval.jsonValue(), EsQueryLogSupport.json(request));

        BoolQuery query = ArticleQueryFactory.build(request);

        SearchRequest search = new SearchRequest.Builder()
                .index(properties.getIndexName())
                .query(q -> q.bool(query))
                .size(0)
                .trackTotalHits(t -> t.enabled(true))
                .aggregations(AGG_TREND, a -> a
                        .dateHistogram(d -> d
                                .field("created_at")
                                .calendarInterval(interval)
                                .minDocCount(1)
                                .timeZone(SITE_TIME_ZONE)
                                .format(SITE_TIME_FORMAT))
                        .aggregations(AGG_TREND_SITES, sa -> sa
                                .terms(t -> t.field("siteId").size(SITE_BUCKET_SIZE))))
                .build();

        SearchResponse<Void> response = execute(search, "stats/site-trend");
        ArticleTrendStatsResponse result = new ArticleTrendStatsResponse();
        result.setTotal(response.hits().total() == null ? 0 : response.hits().total().value());
        result.setTook(response.took());
        result.setTimeField("createdAt");
        result.setInterval(interval.jsonValue());

        Aggregate trend = response.aggregations().get(AGG_TREND);
        if (trend == null || !trend.isDateHistogram()) {
            result.setBuckets(List.of());
            return result;
        }
        result.setBuckets(trend.dateHistogram().buckets().array().stream()
                .map(bucket -> new ArticleTrendStatsResponse.TrendBucket(
                        bucket.keyAsString() == null ? bucket.key() : bucket.keyAsString(),
                        bucket.keyAsString(),
                        bucket.docCount(),
                        0,
                        readBuckets(bucket.aggregations().get(AGG_TREND_SITES))))
                .toList());
        return result;
    }

    public ArticleTrendStatsResponse trendStats(ArticleTrendStatsRequest request) {
        TimeFieldSpec timeField = resolveTimeField(request.getTimeField());
        CalendarInterval interval = resolveInterval(request.getInterval());
        log.info("ES 统计参数[趋势]: index={}, timeField={}, interval={}, request={}",
                properties.getIndexName(), timeField.apiName(), interval.jsonValue(),
                EsQueryLogSupport.json(request));

        BoolQuery query = ArticleQueryFactory.build(request);

        SearchRequest search = new SearchRequest.Builder()
                .index(properties.getIndexName())
                .query(q -> q.bool(query))
                .size(0)
                .trackTotalHits(t -> t.enabled(true))
                .aggregations(AGG_TREND, a -> a
                        .dateHistogram(d -> d
                                .field(timeField.esField())
                                .calendarInterval(interval)
                                .minDocCount(1))
                        .aggregations(AGG_TREND_SITES, sa -> sa
                                .terms(t -> t.field("siteId").size(SITE_BUCKET_SIZE)))
                        .aggregations(SUM_INTERACTION, sa -> sa
                                .sum(s -> s.field("interaction_count"))))
                .build();

        return toTrendResponse(execute(search, "stats/trend"), timeField, interval);
    }

    private ArticleTrendStatsResponse toTrendResponse(SearchResponse<Void> response,
                                                      TimeFieldSpec timeField,
                                                      CalendarInterval interval) {
        ArticleTrendStatsResponse result = new ArticleTrendStatsResponse();
        result.setTotal(response.hits().total() == null ? 0 : response.hits().total().value());
        result.setTook(response.took());
        result.setTimeField(timeField.apiName());
        result.setInterval(interval.jsonValue());

        Aggregate trend = response.aggregations().get(AGG_TREND);
        if (trend == null || !trend.isDateHistogram()) {
            result.setBuckets(List.of());
            return result;
        }
        result.setBuckets(trend.dateHistogram().buckets().array().stream()
                .map(bucket -> new ArticleTrendStatsResponse.TrendBucket(
                        bucket.key(),
                        bucket.keyAsString(),
                        bucket.docCount(),
                        readSum(bucket.aggregations(), SUM_INTERACTION),
                        readBuckets(bucket.aggregations().get(AGG_TREND_SITES))))
                .toList());
        return result;
    }

    private ArticleSumStatsResponse toSumResponse(SearchResponse<Void> response) {
        ArticleSumStatsResponse result = new ArticleSumStatsResponse();
        result.setTotal(response.hits().total() == null ? 0 : response.hits().total().value());
        result.setTook(response.took());
        result.setSums(readSums(response.aggregations()));

        Aggregate byEmotion = response.aggregations().get(AGG_EMOTION_SUMS);
        if (byEmotion == null || !byEmotion.isSterms()) {
            result.setByEmotion(List.of());
        } else {
            result.setByEmotion(byEmotion.sterms().buckets().array().stream()
                    .map(bucket -> new ArticleSumStatsResponse.EmotionSumBucket(
                            bucket.key().stringValue(), bucket.docCount(), readSums(bucket.aggregations())))
                    .toList());
        }
        return result;
    }

    private ArticleSumStatsResponse.Sums readSums(Map<String, Aggregate> aggregations) {
        ArticleSumStatsResponse.Sums sums = new ArticleSumStatsResponse.Sums();
        sums.setRepostCount(readSum(aggregations, SUM_REPOST));
        sums.setCommentCount(readSum(aggregations, SUM_COMMENT));
        sums.setLikeCount(readSum(aggregations, SUM_LIKE));
        sums.setFollowerCount(readSum(aggregations, SUM_FOLLOWER));
        sums.setCollectCount(readSum(aggregations, SUM_COLLECT));
        return sums;
    }

    private long readSum(Map<String, Aggregate> aggregations, String name) {
        Aggregate aggregate = aggregations.get(name);
        return aggregate == null || !aggregate.isSum() ? 0 : (long) aggregate.sum().value();
    }

    private ArticleStatsResponse toResponse(SearchResponse<Void> response) {
        ArticleStatsResponse result = new ArticleStatsResponse();
        result.setTotal(response.hits().total() == null ? 0 : response.hits().total().value());
        result.setTook(response.took());
        result.setSources(readBuckets(response.aggregations().get(AGG_SOURCES)));
        result.setEmotions(readBuckets(response.aggregations().get(AGG_EMOTIONS)));
        result.setRiskLevels(readBuckets(response.aggregations().get(AGG_RISK_LEVELS)));
        return result;
    }

    private List<TermBucket> readBuckets(Aggregate aggregate) {
        if (aggregate == null || !aggregate.isSterms()) {
            return List.of();
        }
        return aggregate.sterms().buckets().array().stream()
                .map(bucket -> new TermBucket(bucket.key().stringValue(), bucket.docCount()))
                .toList();
    }

    private int resolveTopSize(Integer topSize) {
        int value = topSize == null ? DEFAULT_TOP_SIZE : topSize;
        if (value < 1 || value > MAX_TOP_SIZE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "topSize 必须在 1 到 " + MAX_TOP_SIZE + " 之间");
        }
        return value;
    }

    private FieldSpec resolveField(String field) {
        if (!StringUtils.hasText(field)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "field 不能为空");
        }
        String key = field.trim().replace("_", "").toLowerCase(Locale.ROOT);
        FieldSpec spec = FIELD_SPECS.get(key);
        if (spec == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "不支持的统计字段: " + field + "，可选: " + SUPPORTED_FIELDS);
        }
        return spec;
    }

    private TimeFieldSpec resolveTimeField(String timeField) {
        if (!StringUtils.hasText(timeField)) {
            return TIME_FIELDS.get("createdat");
        }
        String key = timeField.trim().replace("_", "").toLowerCase(Locale.ROOT);
        TimeFieldSpec spec = TIME_FIELDS.get(key);
        if (spec == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "不支持的统计时间字段: " + timeField + "，可选: createdAt, crawlerTime");
        }
        return spec;
    }

    private CalendarInterval resolveSiteTrendInterval(String interval) {
        CalendarInterval value = resolveInterval(interval);
        if (value != CalendarInterval.Hour && value != CalendarInterval.Day
                && value != CalendarInterval.Month) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "站点趋势统计间隔仅支持 hour, day, month");
        }
        return value;
    }

    private CalendarInterval resolveInterval(String interval) {
        if (!StringUtils.hasText(interval)) {
            return CalendarInterval.Day;
        }
        CalendarInterval value = INTERVALS.get(interval.trim().toLowerCase(Locale.ROOT));
        if (value == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "不支持的统计间隔: " + interval + "，可选: hour, day, week, month");
        }
        return value;
    }

    private SearchResponse<Void> execute(SearchRequest search, String queryName) {
        log.debug("ES 统计 DSL [{}]: {}", queryName, search);

        long startedAt = System.currentTimeMillis();
        try {
            SearchResponse<Void> response = client.search(search, Void.class);
            long total = response.hits().total() == null ? 0 : response.hits().total().value();
            log.info("ES 统计完成[{}]: index={}, total={}, took={}ms, cost={}ms",
                    queryName, properties.getIndexName(), total, response.took(),
                    System.currentTimeMillis() - startedAt);
            return response;
        } catch (ElasticsearchException e) {
            log.error("ES 统计查询失败[{}]: index={}, cost={}ms, message={}",
                    queryName, properties.getIndexName(), System.currentTimeMillis() - startedAt,
                    e.getMessage());
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "Elasticsearch 统计查询失败: " + e.getMessage(), e);
        } catch (IOException e) {
            log.error("ES 统计连接失败[{}]: index={}, cost={}ms, message={}",
                    queryName, properties.getIndexName(), System.currentTimeMillis() - startedAt,
                    e.getMessage());
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Elasticsearch 连接失败", e);
        }
    }
}
