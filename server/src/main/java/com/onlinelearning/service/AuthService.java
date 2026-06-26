package com.onlinelearning.service;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.onlinelearning.entity.User;
import com.onlinelearning.mapper.UserMapper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
public class AuthService {
    @Resource private UserMapper userMapper;

    public Map<String, Object> login(String account, String password) {
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, account)
                .or()
                .eq(User::getPhone, account)
                .or()
                .eq(User::getEmail, account)
                .last("limit 1"));
        if (user == null || !String.valueOf(user.getPassword()).equals(password)) {
            throw new IllegalArgumentException("账号或密码错误");
        }
        if ("BANNED".equals(user.getStatus())) {
            throw new IllegalArgumentException("账号已封禁，请联系管理员");
        }
        StpUtil.login(user.getId());
        Map<String, Object> result = new HashMap<String, Object>();
        result.put("token", StpUtil.getTokenValue());
        result.put("user", user);
        return result;
    }

    public void register(User user) {
        if (!StringUtils.hasText(user.getUsername()) || !StringUtils.hasText(user.getPassword())) {
            throw new IllegalArgumentException("用户名和密码不能为空");
        }
        Long count = userMapper.selectCount(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, user.getUsername()));
        if (count != null && count > 0) {
            throw new IllegalArgumentException("用户名已存在");
        }
        user.setRole(StringUtils.hasText(user.getRole()) ? user.getRole() : "STUDENT");
        user.setStatus("ACTIVE");
        user.setCreatedAt(LocalDateTime.now());
        userMapper.insert(user);
    }

    public void forgotPassword(String account, String realName, String newPassword) {
        if (!StringUtils.hasText(account) || !StringUtils.hasText(realName) || !StringUtils.hasText(newPassword)) {
            throw new IllegalArgumentException("账号、姓名和新密码不能为空");
        }
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, account)
                .or()
                .eq(User::getPhone, account)
                .or()
                .eq(User::getEmail, account)
                .last("limit 1"));
        if (user == null) {
            throw new IllegalArgumentException("未找到账号");
        }
        if (!realName.trim().equals(user.getRealName())) {
            throw new IllegalArgumentException("姓名与账号不匹配");
        }
        user.setPassword(newPassword);
        userMapper.updateById(user);
    }
}
