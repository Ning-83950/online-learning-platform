package com.onlinelearning.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.onlinelearning.common.ApiResult;
import com.onlinelearning.entity.User;
import com.onlinelearning.mapper.UserMapper;
import com.onlinelearning.service.AuthService;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    @Resource private AuthService authService;
    @Resource private UserMapper userMapper;

    @PostMapping("/login")
    public ApiResult<Map<String, Object>> login(@RequestBody Map<String, String> body) {
        return ApiResult.ok(authService.login(body.get("account"), body.get("password")));
    }

    @PostMapping("/register")
    public ApiResult<Void> register(@RequestBody User user) {
        authService.register(user);
        return ApiResult.ok();
    }

    @PostMapping("/forgot-password")
    public ApiResult<Void> forgotPassword(@RequestBody Map<String, String> body) {
        authService.forgotPassword(body.get("account"), body.get("realName"), body.get("newPassword"));
        return ApiResult.ok();
    }

    @PostMapping("/logout")
    public ApiResult<Void> logout() {
        StpUtil.logout();
        return ApiResult.ok();
    }

    @GetMapping("/me")
    public ApiResult<User> me() {
        return ApiResult.ok(userMapper.selectById(StpUtil.getLoginIdAsLong()));
    }
}
