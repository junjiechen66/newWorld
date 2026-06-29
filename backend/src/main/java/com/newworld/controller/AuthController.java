package com.newworld.controller;

import com.newworld.common.Result;
import com.newworld.dto.ChangePasswordRequest;
import com.newworld.dto.LoginRequest;
import com.newworld.dto.UpdateUserRequest;
import com.newworld.entity.User;
import com.newworld.service.AuthService;
import com.newworld.config.AuthInterceptor;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.HashMap;
import java.util.Map;

@Tag(name = "认证管理", description = "用户登录/注册/信息查询")
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private AuthService authService;

    @Operation(summary = "用户登录")
    @PostMapping("/login")
    public Result<Map<String, String>> login(@Valid @RequestBody LoginRequest request) {
        String token = authService.login(request);
        Map<String, String> data = new HashMap<>();
        data.put("token", token);
        return Result.success("登录成功", data);
    }

    @Operation(summary = "用户注册")
    @PostMapping("/register")
    public Result<Void> register(@Valid @RequestBody LoginRequest request) {
        authService.register(request);
        return Result.success("注册成功");
    }

    @Operation(summary = "获取当前用户信息")
    @GetMapping("/user-info")
    public Result<User> getUserInfo() {
        Long userId = AuthInterceptor.getCurrentUserId();
        User user = authService.getUserInfo(userId);
        return Result.success(user);
    }

    @Operation(summary = "修改用户信息")
    @PutMapping("/user-info")
    public Result<Void> updateUserInfo(@RequestBody UpdateUserRequest request) {
        Long userId = AuthInterceptor.getCurrentUserId();
        authService.updateUserInfo(userId, request.getNickname(), request.getAvatar());
        return Result.success("修改成功");
    }

    @Operation(summary = "修改密码")
    @PutMapping("/password")
    public Result<Void> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        Long userId = AuthInterceptor.getCurrentUserId();
        authService.changePassword(userId, request.getOldPassword(), request.getNewPassword());
        return Result.success("密码修改成功");
    }

    @Operation(summary = "退出登录")
    @PostMapping("/logout")
    public Result<Void> logout() {
        return Result.success("退出成功");
    }
}
