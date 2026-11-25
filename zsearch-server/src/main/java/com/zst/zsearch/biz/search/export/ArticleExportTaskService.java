package com.zst.zsearch.biz.search.export;

import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zst.zsearch.auth.security.AuthUtils;
import com.zst.zsearch.auth.security.LoginUser;
import com.zst.zsearch.biz.mapper.ExportTaskMapper;
import com.zst.zsearch.biz.model.entity.ExportTask;
import com.zst.zsearch.es.dto.ArticleSearchRequest;
import com.zst.zsearch.es.service.ArticleSearchService;
import com.zst.zsearch.es.support.EsQueryLogSupport;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;

@Slf4j
@Service
public class ArticleExportTaskService {

    private static final int LIST_LIMIT = 50;
    private static final DateTimeFormatter TASK_NAME_TIME = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final ExportTaskMapper exportTaskMapper;
    private final ArticleSearchService articleSearchService;
    private final ArticleExportConditionResolver conditionResolver;
    private final ArticleExportTaskRunner taskRunner;

    public ArticleExportTaskService(ExportTaskMapper exportTaskMapper,
                                    ArticleSearchService articleSearchService,
                                    ArticleExportConditionResolver conditionResolver,
                                    ArticleExportTaskRunner taskRunner) {
        this.exportTaskMapper = exportTaskMapper;
        this.articleSearchService = articleSearchService;
        this.conditionResolver = conditionResolver;
        this.taskRunner = taskRunner;
    }

    public ExportTaskVo create(ArticleExportRequest request) {
        ExportScope scope = ExportScope.from(request.getScope());
        if (scope != ExportScope.ALL) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "离线导出仅支持导出全部数据");
        }
        ExportColumn.resolve(request.getFields());
        ArticleSearchRequest searchRequest = conditionResolver.resolve(request);
        long total = articleSearchService.count(searchRequest);
        if (total <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "没有符合条件的数据可导出");
        }
        if (total > ExportConstants.MAX_OFFLINE_ROWS) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "满足条件的数据量 " + total + " 超过离线导出上限 "
                            + ExportConstants.MAX_OFFLINE_ROWS + "，请缩小时间范围");
        }

        LoginUser user = AuthUtils.currentUser();
        LocalDateTime now = LocalDateTime.now();
        ExportTask task = new ExportTask();
        task.setMonitorType(request.getMonitorType());
        task.setTaskName("文章导出-" + now.format(TASK_NAME_TIME));
        task.setQueryParams(JSON.toJSONString(request));
        task.setTotalRows((int) total);
        task.setExportedRows(0);
        task.setStatus(ExportTaskStatus.PENDING.getCode());
        task.setCreateUserId(user.id());
        task.setCreateUserName(user.username());
        task.setCreateTime(now);
        task.setUpdateTime(now);
        exportTaskMapper.insert(task);

        log.info("离线导出任务创建: taskId={}, totalRows={}, user={}, request={}",
                task.getId(), total, user.username(), EsQueryLogSupport.json(request));
        taskRunner.run(task.getId());
        return ExportTaskVo.from(task);
    }

    public ExportTaskVo get(Long id) {
        return ExportTaskVo.from(requireOwnedTask(id));
    }

    public List<ExportTaskVo> list() {
        LoginUser user = AuthUtils.currentUser();
        List<ExportTask> tasks = exportTaskMapper.selectList(new LambdaQueryWrapper<ExportTask>()
                .eq(ExportTask::getCreateUserId, user.id())
                .orderByDesc(ExportTask::getId)
                .last("LIMIT " + LIST_LIMIT));
        return tasks.stream().map(ExportTaskVo::from).toList();
    }

    public void download(Long id, HttpServletResponse response) throws IOException {
        ExportTask task = requireOwnedTask(id);
        if (!Integer.valueOf(ExportTaskStatus.SUCCESS.getCode()).equals(task.getStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "任务尚未成功完成，无法下载");
        }
        Path file = ExportConstants.taskFile(id);
        if (!Files.exists(file)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "导出文件不存在，请重新发起导出");
        }

        String fileName = safeFileName(task.getTaskName()) + ExportConstants.FILE_SUFFIX;
        response.setContentType(ExportConstants.XLSX_CONTENT_TYPE);
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION, ExportConstants.contentDisposition(fileName));
        response.setContentLengthLong(Files.size(file));
        try (InputStream inputStream = Files.newInputStream(file)) {
            inputStream.transferTo(response.getOutputStream());
        }
        response.flushBuffer();
        log.info("离线导出下载: taskId={}, file={}, user={}", id, file, task.getCreateUserName());
    }

    private ExportTask requireOwnedTask(Long id) {
        LoginUser user = AuthUtils.currentUser();
        ExportTask task = exportTaskMapper.selectById(id);
        if (task == null || !Objects.equals(task.getCreateUserId(), user.id())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "导出任务不存在: " + id);
        }
        return task;
    }

    private static String safeFileName(String taskName) {
        String name = taskName == null || taskName.isBlank() ? "文章导出" : taskName.trim();
        return name.replaceAll("[\\\\/:*?\"<>|]", "_");
    }
}