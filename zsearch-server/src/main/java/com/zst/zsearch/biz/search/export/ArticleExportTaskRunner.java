package com.zst.zsearch.biz.search.export;

import com.zst.zsearch.es.dto.Article;
import com.zst.zsearch.es.dto.ArticleExportBatch;
import com.zst.zsearch.es.dto.ArticleSearchRequest;
import com.zst.zsearch.es.service.ArticleSearchService;
import co.elastic.clients.elasticsearch._types.FieldValue;
import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.zst.zsearch.biz.mapper.ExportTaskMapper;
import com.zst.zsearch.biz.model.entity.ExportTask;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
public class ArticleExportTaskRunner {

    private static final int ERROR_MSG_MAX_LENGTH = 1_000;

    private final ExportTaskMapper exportTaskMapper;
    private final ArticleSearchService articleSearchService;
    private final ArticleExportConditionResolver conditionResolver;

    public ArticleExportTaskRunner(ExportTaskMapper exportTaskMapper,
                                   ArticleSearchService articleSearchService,
                                   ArticleExportConditionResolver conditionResolver) {
        this.exportTaskMapper = exportTaskMapper;
        this.articleSearchService = articleSearchService;
        this.conditionResolver = conditionResolver;
    }

    @Async("exportTaskExecutor")
    public void run(Long taskId) {
        try {
            execute(taskId);
        } catch (Exception e) {
            log.error("离线导出任务异常: taskId={}", taskId, e);
            markFailed(taskId, e);
        }
    }

    /**
     * 服务重启后，遗留的待处理/处理中任务无法续跑，统一标记失败
     */
    @EventListener(ApplicationReadyEvent.class)
    public void recoverInterruptedTasks() {
        int count = exportTaskMapper.update(null, new UpdateWrapper<ExportTask>()
                .in("status", ExportTaskStatus.PENDING.getCode(), ExportTaskStatus.RUNNING.getCode())
                .set("status", ExportTaskStatus.FAILED.getCode())
                .set("error_msg", "服务重启，任务已中断，请重新发起导出")
                .set("update_time", LocalDateTime.now()));
        if (count > 0) {
            log.warn("离线导出任务恢复: 已将 {} 个中断任务标记为失败", count);
        }
    }

    private void execute(Long taskId) {
        ExportTask task = exportTaskMapper.selectById(taskId);
        if (task == null) {
            log.warn("离线导出任务不存在: taskId={}", taskId);
            return;
        }
        if (!markRunning(taskId)) {
            log.warn("离线导出任务状态不是待处理，跳过: taskId={}, status={}", taskId, task.getStatus());
            return;
        }

        Path file = ExportConstants.taskFile(taskId);
        try {
            ExportConstants.ensureExportDir();
            ArticleExportRequest request = JSON.parseObject(task.getQueryParams(), ArticleExportRequest.class);
            if (request == null) {
                throw new IllegalStateException("导出条件快照解析失败");
            }
            ArticleSearchRequest searchRequest = conditionResolver.resolve(request);
            List<ExportColumn> columns = ExportColumn.resolve(request.getFields());
            List<String> esFields = ExportColumn.esFields(columns);
            log.info("离线导出开始: taskId={}, totalRows={}, columns={}",
                    taskId, task.getTotalRows(), columns.size());

            int exported = writeWorkbook(taskId, file, searchRequest, columns, esFields);
            markSuccess(taskId, exported);
            log.info("离线导出完成: taskId={}, exported={}, file={}", taskId, exported, file);
        } catch (Exception e) {
            log.error("离线导出失败: taskId={}", taskId, e);
            deleteQuietly(file);
            markFailed(taskId, e);
        }
    }

    private int writeWorkbook(Long taskId, Path file, ArticleSearchRequest searchRequest,
                              List<ExportColumn> columns, List<String> esFields) throws Exception {
        int exported = 0;
        List<FieldValue> searchAfter = null;
        try (ArticleExcelWriter writer = new ArticleExcelWriter(columns);
             OutputStream outputStream = Files.newOutputStream(file)) {
            while (exported < ExportConstants.MAX_OFFLINE_ROWS) {
                int batchSize = Math.min(ExportConstants.SCAN_BATCH_SIZE,
                        ExportConstants.MAX_OFFLINE_ROWS - exported);
                ArticleExportBatch batch = articleSearchService.getArticleList(
                        searchRequest, searchAfter, batchSize, esFields);
                List<Article> articles = batch.articles();
                if (articles.isEmpty()) {
                    break;
                }
                writer.append(articles);
                exported += articles.size();
                updateProgress(taskId, exported);
                searchAfter = batch.nextSearchAfter();
                if (searchAfter == null || articles.size() < batchSize) {
                    break;
                }
            }
            writer.writeTo(outputStream);
        }
        return exported;
    }

    private boolean markRunning(Long taskId) {
        return exportTaskMapper.update(null, new UpdateWrapper<ExportTask>()
                .eq("id", taskId)
                .eq("status", ExportTaskStatus.PENDING.getCode())
                .set("status", ExportTaskStatus.RUNNING.getCode())
                .set("update_time", LocalDateTime.now())) == 1;
    }

    private void updateProgress(Long taskId, int exported) {
        exportTaskMapper.update(null, new UpdateWrapper<ExportTask>()
                .eq("id", taskId)
                .set("exported_rows", exported)
                .set("update_time", LocalDateTime.now()));
    }

    private void markSuccess(Long taskId, int exported) {
        exportTaskMapper.update(null, new UpdateWrapper<ExportTask>()
                .eq("id", taskId)
                .set("status", ExportTaskStatus.SUCCESS.getCode())
                .set("exported_rows", exported)
                .set("error_msg", null)
                .set("update_time", LocalDateTime.now()));
    }

    private void markFailed(Long taskId, Exception e) {
        String message = e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
        if (message.length() > ERROR_MSG_MAX_LENGTH) {
            message = message.substring(0, ERROR_MSG_MAX_LENGTH);
        }
        exportTaskMapper.update(null, new UpdateWrapper<ExportTask>()
                .eq("id", taskId)
                .set("status", ExportTaskStatus.FAILED.getCode())
                .set("error_msg", message)
                .set("update_time", LocalDateTime.now()));
    }

    private void deleteQuietly(Path file) {
        try {
            Files.deleteIfExists(file);
        } catch (Exception e) {
            log.warn("删除失败的导出文件异常: file={}", file, e);
        }
    }
}