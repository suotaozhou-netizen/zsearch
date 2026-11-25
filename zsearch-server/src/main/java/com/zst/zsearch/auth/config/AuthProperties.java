package com.zst.zsearch.auth.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "zsearch.auth")
public class AuthProperties {

    private String jwtSecret;

    private long tokenTtlMinutes = 30;
}