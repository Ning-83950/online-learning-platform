package com.onlinelearning.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.onlinelearning.common.ApiResult;
import com.onlinelearning.common.PageResult;
import com.onlinelearning.common.StatusText;
import com.onlinelearning.entity.User;
import com.onlinelearning.mapper.UserMapper;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/api/users")
public class UserController {
    @Resource private UserMapper userMapper;

    @GetMapping("/page")
    public ApiResult<PageResult<Map<String, Object>>> page(@RequestParam(defaultValue = "1") Long pageNum,
                                                           @RequestParam(defaultValue = "10") Long pageSize,
                                                           @RequestParam(required = false) String keyword,
                                                           @RequestParam(required = false) String role,
                                                           @RequestParam(required = false) String status) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<User>().orderByDesc(User::getCreatedAt);
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(User::getUsername, keyword).or().like(User::getRealName, keyword)
                    .or().like(User::getPhone, keyword).or().like(User::getEmail, keyword));
        }
        if (StringUtils.hasText(role)) wrapper.eq(User::getRole, role);
        if (StringUtils.hasText(status)) wrapper.eq(User::getStatus, status);
        Page<User> page = userMapper.selectPage(new Page<User>(pageNum, pageSize), wrapper);
        List<Map<String, Object>> records = new ArrayList<Map<String, Object>>();
        for (User item : page.getRecords()) {
            Map<String, Object> map = new HashMap<String, Object>();
            map.put("id", item.getId());
            map.put("username", item.getUsername());
            map.put("realName", item.getRealName());
            map.put("role", item.getRole());
            map.put("roleText", roleText(item.getRole()));
            map.put("phone", item.getPhone());
            map.put("email", item.getEmail());
            map.put("avatar", item.getAvatar());
            map.put("status", item.getStatus());
            map.put("statusText", StatusText.of(item.getStatus()));
            map.put("createdAt", item.getCreatedAt());
            records.add(map);
        }
        return ApiResult.ok(new PageResult<Map<String, Object>>(page.getTotal(), pageNum, pageSize, records));
    }

    @GetMapping("/{id}")
    public ApiResult<User> detail(@PathVariable Long id) {
        return ApiResult.ok(userMapper.selectById(id));
    }

    @PostMapping
    public ApiResult<Void> add(@RequestBody User user) {
        user.setCreatedAt(LocalDateTime.now());
        userMapper.insert(user);
        return ApiResult.ok();
    }

    @PutMapping
    public ApiResult<Void> update(@RequestBody User user) {
        userMapper.updateById(user);
        return ApiResult.ok();
    }

    @PostMapping("/{id}/ban")
    public ApiResult<Void> ban(@PathVariable Long id) {
        User user = userMapper.selectById(id);
        user.setStatus("BANNED");
        userMapper.updateById(user);
        return ApiResult.ok();
    }

    @PostMapping("/{id}/enable")
    public ApiResult<Void> enable(@PathVariable Long id) {
        User user = userMapper.selectById(id);
        user.setStatus("ACTIVE");
        userMapper.updateById(user);
        return ApiResult.ok();
    }

    @DeleteMapping("/{id}")
    public ApiResult<Void> delete(@PathVariable Long id) {
        userMapper.deleteById(id);
        return ApiResult.ok();
    }

    private String roleText(String role) {
        if ("ADMIN".equals(role)) return "管理员";
        if ("TEACHER".equals(role)) return "教师";
        if ("STUDENT".equals(role)) return "学习者";
        return role;
    }
}
