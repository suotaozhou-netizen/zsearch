package com.zst.zsearch.auth.security;

import cn.hutool.jwt.JWT;
import cn.hutool.jwt.JWTUtil;
import cn.hutool.jwt.JWTValidator;
import cn.hutool.jwt.signers.JWTSigner;
import cn.hutool.jwt.signers.JWTSignerUtil;
import com.zst.zsearch.auth.config.AuthProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
public class JwtTokenProvider {

    private static final Logger log = LoggerFactory.getLogger(JwtTokenProvider.class);
    private static final String CLAIM_USER_ID = "uid";

    private final JWTSigner signer;
    private final long ttlSeconds;

    public JwtTokenProvider(AuthProperties properties) {
        String secret = properties.getJwtSecret();
        if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException("zsearch.auth.jwt-secret 长度至少需要 32 字节");
        }
        this.signer = JWTSignerUtil.hs256(secret.getBytes(StandardCharsets.UTF_8));
        this.ttlSeconds = Math.max(60, properties.getTokenTtlMinutes() * 60);
    }

    public long getTtlSeconds() {
        return ttlSeconds;
    }

    public String createToken(LoginUser user) {
        Date now = new Date();
        return JWT.create()
                .setPayload("sub", user.username())
                .setPayload(CLAIM_USER_ID, user.id())
                .setIssuedAt(now)
                .setExpiresAt(new Date(now.getTime() + ttlSeconds * 1000))
                .setSigner(signer)
                .sign();
    }

    public LoginUser parse(String token) {
        try {
            JWT jwt = JWTUtil.parseToken(token);
            if (!jwt.setSigner(signer).verify()) {
                return null;
            }
            JWTValidator.of(jwt).validateAlgorithm(signer).validateDate();

            Object subject = jwt.getPayload("sub");
            Object userId = jwt.getPayload(CLAIM_USER_ID);
            if (!(subject instanceof String username) || username.isBlank() || userId == null) {
                return null;
            }
            Integer id = parseUserId(userId);
            if (id == null) {
                return null;
            }
            return new LoginUser(id, username, null);
        } catch (RuntimeException e) {
            log.debug("JWT 解析失败: {}", e.getMessage());
            return null;
        }
    }

    private Integer parseUserId(Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        try {
            return Integer.valueOf(String.valueOf(value));
        } catch (NumberFormatException e) {
            return null;
        }
    }
}