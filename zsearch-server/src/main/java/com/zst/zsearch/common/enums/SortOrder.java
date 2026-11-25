package com.zst.zsearch.common.enums;

import lombok.Getter;

@Getter
public enum SortOrder {

    ASC("asc"),
    DESC("desc");

    private final String value;

    SortOrder(String value) {
        this.value = value;
    }

    public static SortOrder from(String value) {
        if (value == null || value.isBlank()) {
            return DESC;
        }
        for (SortOrder order : values()) {
            if (order.value.equalsIgnoreCase(value.trim())) {
                return order;
            }
        }
        throw new IllegalArgumentException("不支持的排序方向: " + value);
    }
}
