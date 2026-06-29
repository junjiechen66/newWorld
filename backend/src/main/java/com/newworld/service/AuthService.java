package com.newworld.service;

import com.newworld.entity.User;
import com.newworld.dto.LoginRequest;

public interface AuthService {

    /**
     * 用户注册
     */
    void register(LoginRequest request);

    /**
     * 用户登录
     * @return token
     */
    String login(LoginRequest request);

    /**
     * 获取当前用户信息
     */
    User getUserInfo(Long userId);

    /**
     * 修改用户信息
     */
    void updateUserInfo(Long userId, String nickname, String avatar);

    /**
     * 修改密码
     */
    void changePassword(Long userId, String oldPassword, String newPassword);
}
