package com.zst.zsearch.auth.security;

import com.zst.zsearch.auth.mapper.SysUserMapper;
import com.zst.zsearch.auth.model.entity.SysUser;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";
    private static final int STATUS_NORMAL = 0;

    private final JwtTokenProvider tokenProvider;
    private final SysUserMapper sysUserMapper;

    public JwtAuthenticationFilter(JwtTokenProvider tokenProvider, SysUserMapper sysUserMapper) {
        this.tokenProvider = tokenProvider;
        this.sysUserMapper = sysUserMapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (StringUtils.hasText(header) && header.startsWith(BEARER_PREFIX)
                && SecurityContextHolder.getContext().getAuthentication() == null) {
            String token = header.substring(BEARER_PREFIX.length()).trim();
            LoginUser tokenUser = tokenProvider.parse(token);
            if (tokenUser != null) {
                SysUser user = sysUserMapper.selectById(tokenUser.id());
                if (isActive(user) && user.getUsername().equals(tokenUser.username())) {
                    LoginUser loginUser = new LoginUser(user.getId(), user.getUsername(), user.getNickname());
                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(loginUser, null,
                                    List.of(new SimpleGrantedAuthority("ROLE_USER")));
                    authentication.setDetails(
                            new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
            }
        }
        filterChain.doFilter(request, response);
    }

    private boolean isActive(SysUser user) {
        return user != null && Integer.valueOf(STATUS_NORMAL).equals(user.getStatus());
    }
}