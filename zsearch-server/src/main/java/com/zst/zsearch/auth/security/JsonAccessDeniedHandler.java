package com.zst.zsearch.auth.security;

import com.zst.zsearch.common.ResultCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class JsonAccessDeniedHandler implements AccessDeniedHandler {

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException {
        AuthResponseWriter.write(response, ResultCode.FORBIDDEN.getCode(),
                AuthResponseWriter.defaultMessage(ResultCode.FORBIDDEN.getCode()));
    }
}