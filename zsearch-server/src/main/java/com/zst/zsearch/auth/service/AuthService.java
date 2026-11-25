package com.zst.zsearch.auth.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zst.zsearch.auth.dto.LoginRequest;
import com.zst.zsearch.auth.mapper.SysUserMapper;
import com.zst.zsearch.auth.model.entity.SysUser;
import com.zst.zsearch.auth.security.AuthUtils;
import com.zst.zsearch.auth.security.JwtTokenProvider;
import com.zst.zsearch.auth.security.LoginUser;
import com.zst.zsearch.auth.vo.LoginVo;
import com.zst.zsearch.auth.vo.UserVo;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AuthService {

    private static final int STATUS_NORMAL = 0;

    private final SysUserMapper sysUserMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;

    public AuthService(SysUserMapper sysUserMapper,
                       PasswordEncoder passwordEncoder,
                       JwtTokenProvider tokenProvider) {
        this.sysUserMapper = sysUserMapper;
        this.passwordEncoder = passwordEncoder;
        this.tokenProvider = tokenProvider;
    }

    public LoginVo login(LoginRequest request) {
        String username = request.getUsername().trim();
        SysUser user = sysUserMapper.selectOne(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, username));
        if (user == null || !passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "用户名或密码错误");
        }
        if (!Integer.valueOf(STATUS_NORMAL).equals(user.getStatus())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "账号已停用");
        }

        LoginUser loginUser = new LoginUser(user.getId(), user.getUsername(), user.getNickname());
        LoginVo response = new LoginVo();
        response.setToken(tokenProvider.createToken(loginUser));
        response.setExpiresIn(tokenProvider.getTtlSeconds());
        response.setUser(UserVo.from(loginUser));
        return response;
    }

    public LoginVo refresh() {
        LoginUser loginUser = AuthUtils.currentUser();
        LoginVo response = new LoginVo();
        response.setToken(tokenProvider.createToken(loginUser));
        response.setExpiresIn(tokenProvider.getTtlSeconds());
        response.setUser(UserVo.from(loginUser));
        return response;
    }

    public UserVo currentUser() {
        return UserVo.from(AuthUtils.currentUser());
    }
}