package com.zst.zsearch.biz.search.export;

import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public final class ExportConstants {

    /**
     * 离线导出文件目录，暂固定本地路径，后续切换文件系统时替换此处实现
     */
    public static final Path EXPORT_DIR = Path.of("C:/Users/zst12/Desktop/zSearch/files");

    public static final String FILE_SUFFIX = ".xlsx";

    public static final String XLSX_CONTENT_TYPE =
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    /**
     * 实时导出上限（选中/当前页/前1000条）
     */
    public static final int MAX_REALTIME_ROWS = 1_000;

    /**
     * 离线导出上限
     */
    public static final int MAX_OFFLINE_ROWS = 100_000;

    /**
     * 离线导出单次 search_after 扫描批量
     */
    public static final int SCAN_BATCH_SIZE = 2_000;

    private ExportConstants() {
    }

    public static Path taskFile(long taskId) {
        return EXPORT_DIR.resolve(taskId + FILE_SUFFIX);
    }

    public static void ensureExportDir() {
        try {
            Files.createDirectories(EXPORT_DIR);
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "创建导出目录失败: " + EXPORT_DIR, e);
        }
    }

    public static String contentDisposition(String fileName) {
        return ContentDisposition.attachment()
                .filename(fileName, StandardCharsets.UTF_8)
                .build()
                .toString();
    }
}