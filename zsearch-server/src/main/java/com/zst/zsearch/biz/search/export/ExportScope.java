package com.zst.zsearch.biz.search.export;

import lombok.Getter;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.Locale;

@Getter
public enum ExportScope {

    /**
     * 选中的文章
     */
    SELECTED("selected"),

    /**
     * 当前页
     */
    PAGE("page"),

    /**
     * 前1000条
     */
    FIRST_1000("first_1000"),

    /**
     * 全部（离线导出）
     */
    ALL("all");

    private final String value;

    ExportScope(String value) {
        this.value = value;
    }

    public static ExportScope from(String value) {
        if (value == null || value.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "导出范围不能为空");
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        for (ExportScope scope : values()) {
            if (scope.value.equals(normalized) || scope.name().toLowerCase(Locale.ROOT).equals(normalized)) {
                return scope;
            }
        }
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "不支持的导出范围: " + value);
    }
}