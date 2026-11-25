package com.zst.zsearch.common.json;

import com.alibaba.fastjson2.JSON;

public final class JsonCodec {

    private JsonCodec() {
    }

    public static String toJson(Object value) {
        return JSON.toJSONString(value);
    }

    public static <T> T parse(String json, Class<T> type) {
        return JSON.parseObject(json, type);
    }
}
