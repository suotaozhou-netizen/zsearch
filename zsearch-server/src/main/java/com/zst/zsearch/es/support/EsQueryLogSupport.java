package com.zst.zsearch.es.support;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONWriter;

public final class EsQueryLogSupport {

    private EsQueryLogSupport() {
    }

    public static String json(Object value) {
        if (value == null) {
            return "null";
        }
        try {
            return JSON.toJSONString(value, JSONWriter.Feature.WriteMapNullValue);
        } catch (RuntimeException e) {
            return String.valueOf(value);
        }
    }
}