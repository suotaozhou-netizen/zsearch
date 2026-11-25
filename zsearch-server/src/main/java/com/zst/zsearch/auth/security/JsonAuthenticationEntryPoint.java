package com.zst.zsearch.auth.security;

import com.zst.zsearch.common.ResultCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class JsonAuthenticationEntryPoint implements AuthenticationEntryPoint {

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        AuthResponseWriter.write(response, ResultCode.UNAUTHORIZED.getCode(),
                AuthResponseWriter.defaultMessage(ResultCode.UNAUTHORIZED.getCode()));
    }
}