package com.exam.controller;

import com.exam.common.Result;
import com.exam.dto.auth.LoginRequest;
import com.exam.dto.auth.LoginResponse;
import com.exam.dto.auth.RegisterRequest;
import com.exam.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.Collections;
import java.util.Map;

/**
 * 登录 / 注册。这两个接口无需鉴权。
 */
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /**
     * 登录。教师和学生用同一个入口，返回体里的 role 决定前端跳转到哪个界面。
     */
    @PostMapping("/login")
    public Result<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return Result.ok("登录成功", authService.login(request));
    }

    /**
     * 注册。返回新用户 ID。
     */
    @PostMapping("/register")
    public Result<Map<String, Long>> register(@Valid @RequestBody RegisterRequest request) {
        Long userId = authService.register(request);
        return Result.ok("注册成功", Collections.singletonMap("userId", userId));
    }
}
