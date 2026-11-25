package com.zst.zsearch.biz.search.export;

import lombok.Getter;

@Getter
public enum ExportTaskStatus {

    PENDING(0, "待处理"),
    RUNNING(1, "处理中"),
    SUCCESS(2, "成功"),
    FAILED(3, "失败");

    private final int code;
    private final String text;

    ExportTaskStatus(int code, String text) {
        this.code = code;
        this.text = text;
    }

    public static ExportTaskStatus fromCode(int code) {
        for (ExportTaskStatus status : values()) {
            if (status.code == code) {
                return status;
            }
        }
        throw new IllegalArgumentException("未知的导出任务状态: " + code);
    }
}