package com.zst.zsearch.biz.controller;

import com.zst.zsearch.biz.search.export.*;
import com.zst.zsearch.common.ApiResponse;
import com.zst.zsearch.es.support.EsQueryLogSupport;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/export")
public class ArticleExportController {

    private final ArticleExportService articleExportService;
    private final ArticleExportTaskService articleExportTaskService;

    public ArticleExportController(ArticleExportService articleExportService,
                                   ArticleExportTaskService articleExportTaskService) {
        this.articleExportService = articleExportService;
        this.articleExportTaskService = articleExportTaskService;
    }

    @GetMapping("/fields")
    public ApiResponse<List<ExportFieldVo>> fields() {
        return ApiResponse.success(Arrays.stream(ExportColumn.values())
                .map(column -> new ExportFieldVo(column.getKey(), column.getHeader()))
                .toList());
    }

    /**
     * 实时导出：选中 / 当前页 / 前1000条
     */
    @PostMapping("/realtime")
    public void realtime(@Valid @RequestBody ArticleExportRequest request,
                         HttpServletResponse response) throws IOException {
        log.info("实时导出接口参数: {}", EsQueryLogSupport.json(request));
        articleExportService.exportRealtime(request, response);
    }

    /**
     * 创建离线导出任务：导出全部数据（上限10万）
     */
    @PostMapping("/task")
    public ApiResponse<ExportTaskVo> createTask(@Valid @RequestBody ArticleExportRequest request) {
        log.info("离线导出接口参数: {}", EsQueryLogSupport.json(request));
        return ApiResponse.success(articleExportTaskService.create(request));
    }

    @GetMapping("/task/{id}")
    public ApiResponse<ExportTaskVo> task(@PathVariable Long id) {
        return ApiResponse.success(articleExportTaskService.get(id));
    }

    @GetMapping("/tasks")
    public ApiResponse<List<ExportTaskVo>> tasks() {
        return ApiResponse.success(articleExportTaskService.list());
    }

    @GetMapping("/download/{id}")
    public void download(@PathVariable Long id, HttpServletResponse response) throws IOException {
        articleExportTaskService.download(id, response);
    }
}