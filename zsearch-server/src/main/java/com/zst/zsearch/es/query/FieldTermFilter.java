package com.zst.zsearch.es.query;

import java.util.List;

public record FieldTermFilter(String field, List<String> values) {

    public FieldTermFilter {
        values = values == null ? List.of() : List.copyOf(values);
    }
}