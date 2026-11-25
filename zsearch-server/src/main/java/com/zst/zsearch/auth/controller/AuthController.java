package com.zst.zsearch.auth.controller;

import com.zst.zsearch.auth.dto.LoginRequest;
import com.zst.zsearch.auth.service.AuthService;
import com.zst.zsearch.auth.vo.LoginVo;
import com.zst.zsearch.auth.vo.UserVo;
import com.zst.zsearch.common.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public LoginVo login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @PostMapping("/refresh")
    public LoginVo refresh() {
        return authService.refresh();
    }

    @GetMapping("/me")
    public UserVo me() {
        return authService.currentUser();
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout() {
        return ApiResponse.success();
    }
}