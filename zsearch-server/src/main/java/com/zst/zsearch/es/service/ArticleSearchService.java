package com.zst.zsearch.es.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.ElasticsearchException;
import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.SortOptions;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch.core.GetRequest;
import co.elastic.clients.elasticsearch.core.GetResponse;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import com.zst.zsearch.es.config.EsProperties;
import com.zst.zsearch.es.dto.Article;
import com.zst.zsearch.es.dto.ArticleExportBatch;
import com.zst.zsearch.es.dto.ArticleSearchRequest;
import com.zst.zsearch.es.dto.ArticleSearchResponse;
import com.zst.zsearch.es.support.EsQueryLogSupport;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Slf4j
@Service
public class ArticleSearchService {

    public static final int MAX_RESULT_WINDOW = 10_000;
    private static final int EXPORT_SCAN_MAX_SIZE = 2_000;
    private static final int EXPORT_SELECTED_MAX = 1_000;
    private static final String ID_FIELD = "id";
    private static final int DEFAULT_PAGE = 1;
    private static final int DEFAULT_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 100;
    private static final String DEFAULT_SORT = "created_at";
    // value 表示是否允许升序：时间字段可升可降，互动计数字段只允许降序
    private static final Map<String, Boolean> SORT_FIELDS = Map.of(
            "created_at", true,
            "crawler_time", true,
            "_score", true,
            "repost_count", false,
            "comment_count", false,
            "like_count", false,
            "follower_count", false,
            "collect_count", false,
            "interaction_count", false
    );
    private static final Map<String, String> SORT_ALIASES = Map.of(
            "createdAt", "created_at",
            "crawlerTime", "crawler_time",
            "repostCount", "repost_count",
            "commentCount", "comment_count",
            "likeCount", "like_count",
            "followerCount", "follower_count",
            "collectCount", "collect_count",
            "interactionCount", "interaction_count"
    );

    private final ElasticsearchClient client;
    private final EsProperties properties;

    public ArticleSearchService(ElasticsearchClient client, EsProperties properties) {
        this.client = client;
        this.properties = properties;
    }

    public ArticleSearchResponse search(ArticleSearchRequest request) {
        int page = request.getPage() == null ? DEFAULT_PAGE : request.getPage();
        int size = request.getSize() == null ? DEFAULT_SIZE : request.getSize();
        if (page < 1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "page 必须大于等于 1");
        }
        if (size < 1 || size > MAX_PAGE_SIZE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "size 必须在 1 到 " + MAX_PAGE_SIZE + " 之间");
        }
        long from = (long) (page - 1) * size;
        if (from + size > MAX_RESULT_WINDOW) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "分页过深，from + size 不能超过 " + MAX_RESULT_WINDOW + "，请缩小时间范围");
        }

        SortSpec sort = resolveSort(request.getSortBy(), request.getSortOrder());
        String sortBy = sort.field();
        String sortOrder = sort.order();

        BoolQuery bool = ArticleQueryFactory.build(request);

        SearchRequest.Builder search = new SearchRequest.Builder()
                .index(properties.getIndexName())
                .query(q -> q.bool(bool))
                .from((int) from)
                .size(size)
                .trackTotalHits(t -> t.enabled(true))
                .sort(buildSorts(sortBy, sortOrder));

        SearchRequest searchRequest = search.build();
        log.info("ES 检索参数: index={}, page={}, size={}, sort={}:{}, request={}",
                properties.getIndexName(), page, size, sortBy, sortOrder,
                EsQueryLogSupport.json(request));
        log.debug("ES 检索 DSL: {}", searchRequest);

        long startedAt = System.currentTimeMillis();
        try {
            SearchResponse<Article> response = client.search(searchRequest, Article.class);
            long total = response.hits().total() == null ? 0 : response.hits().total().value();
            log.info("ES 检索完成: index={}, total={}, returned={}, took={}ms, cost={}ms",
                    properties.getIndexName(), total, response.hits().hits().size(),
                    response.took(), System.currentTimeMillis() - startedAt);
            return toResponse(response, page, size);
        } catch (ElasticsearchException e) {
            log.error("ES 检索失败: index={}, cost={}ms, message={}",
                    properties.getIndexName(), System.currentTimeMillis() - startedAt, e.getMessage());
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "Elasticsearch 查询失败: " + e.getMessage(), e);
        } catch (IOException e) {
            log.error("ES 检索连接失败: index={}, cost={}ms, message={}",
                    properties.getIndexName(), System.currentTimeMillis() - startedAt, e.getMessage());
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Elasticsearch 连接失败", e);
        }
    }

    public Article getById(String id) {
        if (!StringUtils.hasText(id)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "文章 id 不能为空");
        }
        String articleId = id.trim();
        GetRequest getRequest = GetRequest.of(g -> g
                .index(properties.getIndexName())
                .id(articleId));
        log.info("ES 文章详情参数: index={}, id={}", properties.getIndexName(), articleId);
        log.debug("ES 文章详情 DSL: {}", getRequest);

        long startedAt = System.currentTimeMillis();
        try {
            GetResponse<Article> response = client.get(getRequest, Article.class);
            if (!response.found() || response.source() == null) {
                log.info("ES 文章不存在: index={}, id={}, cost={}ms",
                        properties.getIndexName(), articleId, System.currentTimeMillis() - startedAt);
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "文章不存在: " + articleId);
            }
            Article article = response.source();
            if (!StringUtils.hasText(article.getId())) {
                article.setId(response.id());
            }
            log.info("ES 文章详情查询完成: id={}, cost={}ms",
                    articleId, System.currentTimeMillis() - startedAt);
            return article;
        } catch (ElasticsearchException e) {
            if (e.status() == 404) {
                log.info("ES 文章不存在: index={}, id={}", properties.getIndexName(), articleId);
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "文章不存在: " + articleId, e);
            }
            log.error("ES 文章详情查询失败: index={}, id={}, message={}",
                    properties.getIndexName(), articleId, e.getMessage());
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "Elasticsearch 查询失败: " + e.getMessage(), e);
        } catch (IOException e) {
            log.error("ES 文章详情连接失败: index={}, id={}, message={}",
                    properties.getIndexName(), articleId, e.getMessage());
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Elasticsearch 连接失败", e);
        }
    }

    /**
     * 导出用：按 id 批量获取原始文档，保留请求中的过滤条件
     */
    public List<Article> getArticleList(ArticleSearchRequest request, List<String> ids,
                                        List<String> sourceFields) {
        List<String> normalizedIds = normalizeIds(ids);
        if (normalizedIds.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "请选择要导出的文章");
        }
        if (normalizedIds.size() > EXPORT_SELECTED_MAX) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "选中导出最多支持 " + EXPORT_SELECTED_MAX + " 篇文章");
        }

        BoolQuery bool = new BoolQuery.Builder()
                .must(ArticleQueryFactory.build(request)._toQuery())
                .filter(f -> f.terms(t -> t
                        .field(ID_FIELD)
                        .terms(tf -> tf.value(normalizedIds.stream().map(FieldValue::of).toList()))))
                .build();

        SearchRequest.Builder search = new SearchRequest.Builder()
                .index(properties.getIndexName())
                .query(q -> q.bool(bool))
                .size(normalizedIds.size())
                .trackTotalHits(t -> t.enabled(false));
        applySource(search, sourceFields);

        log.info("ES 导出选中参数: index={}, ids={}, request={}",
                properties.getIndexName(), normalizedIds.size(), EsQueryLogSupport.json(request));
        return toArticles(execute(search.build(), "导出选中查询"));
    }

    /**
     * 导出用：按 from/size 获取当前页或前 N 条原始文档
     */
    public List<Article> getArticleList(ArticleSearchRequest request, int from, int size,
                                        List<String> sourceFields) {
        if (from < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "导出起始位置不能小于 0");
        }
        if (size < 1 || size > EXPORT_SELECTED_MAX) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "导出条数必须在 1 到 " + EXPORT_SELECTED_MAX + " 之间");
        }
        if ((long) from + size > MAX_RESULT_WINDOW) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "分页过深，from + size 不能超过 " + MAX_RESULT_WINDOW + "，请缩小时间范围");
        }

        SortSpec sort = resolveSort(request.getSortBy(), request.getSortOrder());
        SearchRequest.Builder search = new SearchRequest.Builder()
                .index(properties.getIndexName())
                .query(q -> q.bool(ArticleQueryFactory.build(request)))
                .from(from)
                .size(size)
                .trackTotalHits(t -> t.enabled(false))
                .sort(buildSorts(sort.field(), sort.order()));
        applySource(search, sourceFields);

        log.info("ES 导出分页参数: index={}, from={}, size={}, sort={}:{}, request={}",
                properties.getIndexName(), from, size, sort.field(), sort.order(),
                EsQueryLogSupport.json(request));
        return toArticles(execute(search.build(), "导出分页查询"));
    }

    /**
     * 导出用：基于 search_after 的深分页扫描，固定按 created_at desc, id asc 排序稳定翻页
     */
    public ArticleExportBatch getArticleList(ArticleSearchRequest request, List<FieldValue> searchAfter,
                                             int size, List<String> sourceFields) {
        if (size < 1 || size > EXPORT_SCAN_MAX_SIZE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "扫描批量必须在 1 到 " + EXPORT_SCAN_MAX_SIZE + " 之间");
        }
        boolean firstBatch = searchAfter == null || searchAfter.isEmpty();

        SearchRequest.Builder search = new SearchRequest.Builder()
                .index(properties.getIndexName())
                .query(q -> q.bool(ArticleQueryFactory.build(request)))
                .size(size)
                .trackTotalHits(t -> t.enabled(firstBatch))
                .sort(buildSorts(DEFAULT_SORT, "desc"));
        if (!firstBatch) {
            search.searchAfter(searchAfter);
        }
        applySource(search, sourceFields);

        log.info("ES 导出扫描参数: index={}, size={}, firstBatch={}, request={}",
                properties.getIndexName(), size, firstBatch, EsQueryLogSupport.json(request));
        SearchResponse<Article> response = execute(search.build(), "导出扫描查询");

        List<Article> articles = toArticles(response);
        List<FieldValue> nextSearchAfter = null;
        List<Hit<Article>> hits = response.hits().hits();
        if (!hits.isEmpty()) {
            List<FieldValue> sortValues = hits.get(hits.size() - 1).sort();
            if (sortValues != null && !sortValues.isEmpty()) {
                nextSearchAfter = List.copyOf(sortValues);
            }
        }
        long total = firstBatch && response.hits().total() != null
                ? response.hits().total().value()
                : -1L;
        return new ArticleExportBatch(articles, nextSearchAfter, total);
    }

    /**
     * 导出用：统计满足条件的数据量
     */
    public long count(ArticleSearchRequest request) {
        SearchRequest search = new SearchRequest.Builder()
                .index(properties.getIndexName())
                .query(q -> q.bool(ArticleQueryFactory.build(request)))
                .size(0)
                .trackTotalHits(t -> t.enabled(true))
                .build();

        log.info("ES 导出计数参数: index={}, request={}",
                properties.getIndexName(), EsQueryLogSupport.json(request));
        SearchResponse<Article> response = execute(search, "导出计数查询");
        return response.hits().total() == null ? 0 : response.hits().total().value();
    }

    private SortSpec resolveSort(String sortBy, String sortOrder) {
        String field = StringUtils.hasText(sortBy) ? sortBy.trim() : DEFAULT_SORT;
        field = SORT_ALIASES.getOrDefault(field, field);
        Boolean allowAsc = SORT_FIELDS.get(field);
        if (allowAsc == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "不支持的排序字段: " + field);
        }
        String order = StringUtils.hasText(sortOrder)
                ? sortOrder.trim().toLowerCase(Locale.ROOT)
                : "desc";
        if (!"asc".equals(order) && !"desc".equals(order)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "sortOrder 只能是 asc 或 desc");
        }
        if (!allowAsc && "asc".equals(order)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "排序字段 " + field + " 仅支持降序");
        }
        return new SortSpec(field, order);
    }

    private void applySource(SearchRequest.Builder search, List<String> sourceFields) {
        if (sourceFields == null || sourceFields.isEmpty()) {
            return;
        }
        search.source(s -> s.filter(f -> f.includes(sourceFields)));
    }

    private SearchResponse<Article> execute(SearchRequest searchRequest, String queryName) {
        log.debug("ES {} DSL: {}", queryName, searchRequest);
        long startedAt = System.currentTimeMillis();
        try {
            SearchResponse<Article> response = client.search(searchRequest, Article.class);
            long total = response.hits().total() == null ? -1 : response.hits().total().value();
            log.info("ES {}完成: index={}, total={}, returned={}, took={}ms, cost={}ms",
                    queryName, properties.getIndexName(), total, response.hits().hits().size(),
                    response.took(), System.currentTimeMillis() - startedAt);
            return response;
        } catch (ElasticsearchException e) {
            log.error("ES {}失败: index={}, cost={}ms, message={}",
                    queryName, properties.getIndexName(), System.currentTimeMillis() - startedAt,
                    e.getMessage());
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "Elasticsearch 查询失败: " + e.getMessage(), e);
        } catch (IOException e) {
            log.error("ES {}连接失败: index={}, cost={}ms, message={}",
                    queryName, properties.getIndexName(), System.currentTimeMillis() - startedAt,
                    e.getMessage());
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Elasticsearch 连接失败", e);
        }
    }

    private List<Article> toArticles(SearchResponse<Article> response) {
        List<Article> articles = new ArrayList<>(response.hits().hits().size());
        for (Hit<Article> hit : response.hits().hits()) {
            Article document = hit.source();
            if (document == null) {
                continue;
            }
            if (!StringUtils.hasText(document.getId())) {
                document.setId(hit.id());
            }
            articles.add(document);
        }
        return articles;
    }

    private List<String> normalizeIds(List<String> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        return ids.stream()
                .filter(StringUtils::hasText)
                .map(String::trim)
                .distinct()
                .toList();
    }

    private record SortSpec(String field, String order) {
    }

    private List<SortOptions> buildSorts(String sortBy, String sortOrder) {
        SortOrder order = "asc".equals(sortOrder) ? SortOrder.Asc : SortOrder.Desc;
        List<SortOptions> sorts = new ArrayList<>(2);
        if ("_score".equals(sortBy)) {
            sorts.add(SortOptions.of(so -> so.score(sc -> sc.order(order))));
        } else {
            sorts.add(SortOptions.of(so -> so.field(f -> f.field(sortBy).order(order))));
        }
        // 同排序值的文档用 id 兜底，保证 from/size 跨页顺序稳定
        sorts.add(SortOptions.of(so -> so.field(f -> f.field("id").order(SortOrder.Asc))));
        return sorts;
    }

    private ArticleSearchResponse toResponse(SearchResponse<Article> response, int page, int size) {
        long total = response.hits().total() == null ? 0 : response.hits().total().value();
        List<Article> hits = new ArrayList<>(response.hits().hits().size());
        for (Hit<Article> hit : response.hits().hits()) {
            Article document = hit.source();
            if (document == null) {
                continue;
            }
            if (!StringUtils.hasText(document.getId())) {
                document.setId(hit.id());
            }
            document.setScore(hit.score());
            document.setHighlight(hit.highlight());
            hits.add(document);
        }

        ArticleSearchResponse result = new ArticleSearchResponse();
        result.setTotal(total);
        result.setPage(page);
        result.setSize(size);
        long totalPages = (total + size - 1) / size;
        long accessiblePages = (MAX_RESULT_WINDOW - size) / size + 1;
        result.setTotalPages(Math.min(totalPages, accessiblePages));
        result.setTook(response.took());
        result.setArticleList(hits);
        return result;
    }
}
