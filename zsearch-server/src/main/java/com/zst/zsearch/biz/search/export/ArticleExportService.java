package com.zst.zsearch.biz.search.export;

import com.zst.zsearch.es.dto.Article;
import com.zst.zsearch.es.dto.ArticleSearchRequest;
import com.zst.zsearch.es.service.ArticleSearchService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Slf4j
@Service
public class ArticleExportService {

    private static final int DEFAULT_PAGE = 1;
    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 100;
    private static final DateTimeFormatter FILE_TIME = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final ArticleSearchService articleSearchService;
    private final ArticleExportConditionResolver conditionResolver;

    public ArticleExportService(ArticleSearchService articleSearchService,
                                ArticleExportConditionResolver conditionResolver) {
        this.articleSearchService = articleSearchService;
        this.conditionResolver = conditionResolver;
    }

    public void exportRealtime(ArticleExportRequest request, HttpServletResponse response)
            throws IOException {
        ExportScope scope = ExportScope.from(request.getScope());
        if (scope == ExportScope.ALL) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "导出全部数据请使用离线导出");
        }
        List<ExportColumn> columns = ExportColumn.resolve(request.getFields());
        List<String> esFields = ExportColumn.esFields(columns);
        ArticleSearchRequest searchRequest = conditionResolver.resolve(request);
        searchRequest.setSortBy(request.getSortBy());
        searchRequest.setSortOrder(request.getSortOrder());

        List<Article> articles = loadArticles(request, scope, searchRequest, esFields);
        log.info("实时导出完成条件准备: scope={}, rows={}", scope, articles.size());

        String fileName = "文章导出_" + LocalDateTime.now().format(FILE_TIME) + ExportConstants.FILE_SUFFIX;
        response.setContentType(ExportConstants.XLSX_CONTENT_TYPE);
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION, ExportConstants.contentDisposition(fileName));
        try (ArticleExcelWriter writer = new ArticleExcelWriter(columns)) {
            writer.append(articles);
            writer.writeTo(response.getOutputStream());
        }
        response.flushBuffer();
    }

    private List<Article> loadArticles(ArticleExportRequest request, ExportScope scope,
                                       ArticleSearchRequest searchRequest, List<String> esFields) {
        return switch (scope) {
            case SELECTED -> articleSearchService.getArticleList(searchRequest, request.getIds(), esFields);
            case PAGE -> {
                int page = request.getPage() == null ? DEFAULT_PAGE : request.getPage();
                int size = request.getSize() == null ? DEFAULT_PAGE_SIZE : request.getSize();
                if (page < 1) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "page 必须大于等于 1");
                }
                if (size < 1 || size > MAX_PAGE_SIZE) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                            "size 必须在 1 到 " + MAX_PAGE_SIZE + " 之间");
                }
                long from = (long) (page - 1) * size;
                if (from + size > ArticleSearchService.MAX_RESULT_WINDOW) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                            "分页过深，from + size 不能超过 " + ArticleSearchService.MAX_RESULT_WINDOW
                                    + "，请缩小时间范围");
                }
                yield articleSearchService.getArticleList(searchRequest, (int) from, size, esFields);
            }
            case FIRST_1000 -> articleSearchService.getArticleList(searchRequest, 0,
                    ExportConstants.MAX_REALTIME_ROWS, esFields);
            default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "不支持的导出范围: " + scope);
        };
    }
}