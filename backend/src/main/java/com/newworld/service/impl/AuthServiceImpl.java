package com.newworld.service.impl;

import cn.hutool.crypto.digest.DigestUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.newworld.common.JwtUtil;
import com.newworld.common.exception.BusinessException;
import com.newworld.dto.LoginRequest;
import com.newworld.entity.User;
import com.newworld.mapper.UserMapper;
import com.newworld.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class AuthServiceImpl implements AuthService {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private JwtUtil jwtUtil;

    @Override
    public void register(LoginRequest request) {
        // 检查用户名是否已存在
        Long count = userMapper.selectCount(
                new LambdaQueryWrapper<User>().eq(User::getUsername, request.getUsername()));
        if (count > 0) {
            throw new BusinessException("用户名已存在");
        }

        // 创建用户
        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(DigestUtil.sha256Hex(request.getPassword()));
        user.setNickname(request.getUsername());
        userMapper.insert(user);
    }

    @Override
    public String login(LoginRequest request) {
        // 查找用户
        User user = userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getUsername, request.getUsername()));
        if (user == null) {
            throw new BusinessException("用户名或密码错误");
        }

        // 验证密码
        String encryptedPassword = DigestUtil.sha256Hex(request.getPassword());
        if (!user.getPassword().equals(encryptedPassword)) {
            throw new BusinessException("用户名或密码错误");
        }

        // 更新最近登录时间
        user.setLastLoginTime(LocalDateTime.now());
        userMapper.updateById(user);

        // 生成 Token
        return jwtUtil.generateToken(user.getId(), user.getUsername());
    }

    @Override
    public void updateUserInfo(Long userId, String nickname, String avatar) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        if (nickname != null && !nickname.trim().isEmpty()) {
            user.setNickname(nickname);
        }
        if (avatar != null) {
            user.setAvatar(avatar);
        }
        userMapper.updateById(user);
    }

    @Override
    public User getUserInfo(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        user.setPassword(null); // 不返回密码
        return user;
    }

    @Override
    public void changePassword(Long userId, String oldPassword, String newPassword) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        // 验证旧密码
        String encryptedOld = DigestUtil.sha256Hex(oldPassword);
        if (!user.getPassword().equals(encryptedOld)) {
            throw new BusinessException("旧密码不正确");
        }
        // 更新密码
        user.setPassword(DigestUtil.sha256Hex(newPassword));
        userMapper.updateById(user);
    }
}
