package com.zst.zsearch.auth.security;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONWriter;
import com.zst.zsearch.common.ApiResponse;
import com.zst.zsearch.common.ResultCode;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

final class AuthResponseWriter {

    private AuthResponseWriter() {
    }

    static void write(HttpServletResponse response, int status, String message) throws IOException {
        response.setStatus(status);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write(JSON.toJSONString(
                ApiResponse.fail(status, message), JSONWriter.Feature.WriteMapNullValue));
    }

    static String defaultMessage(int status) {
        return status == ResultCode.FORBIDDEN.getCode()
                ? ResultCode.FORBIDDEN.getMessage()
                : "未登录或登录已过期";
    }
}