package com.onlinelearning.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.onlinelearning.common.ApiResult;
import com.onlinelearning.common.PageResult;
import com.onlinelearning.common.StatusText;
import com.onlinelearning.entity.User;
import com.onlinelearning.entity.Discussion;
import com.onlinelearning.mapper.DiscussionMapper;
import com.onlinelearning.service.LookupService;
import com.onlinelearning.service.ScopeService;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/api/discussions")
public class DiscussionController {
    @Resource private DiscussionMapper discussionMapper;
    @Resource private LookupService lookupService;
    @Resource private ScopeService scopeService;

    @GetMapping("/page")
    public ApiResult<PageResult<Map<String, Object>>> page(@RequestParam(defaultValue = "1") Long pageNum,
                                                           @RequestParam(defaultValue = "10") Long pageSize,
                                                           @RequestParam(required = false) Long courseId,
                                                           @RequestParam(required = false) String keyword) {
        LambdaQueryWrapper<Discussion> wrapper = new LambdaQueryWrapper<Discussion>().orderByDesc(Discussion::getCreatedAt);
        if (courseId != null) wrapper.eq(Discussion::getCourseId, courseId);
        if (StringUtils.hasText(keyword)) wrapper.like(Discussion::getContent, keyword);
        User current = scopeService.currentUser();
        if (current != null && "TEACHER".equals(current.getRole())) {
            List<Long> courseIds = scopeService.teacherCourseIds(current.getId());
            if (courseIds.isEmpty()) {
                return ApiResult.ok(new PageResult<Map<String, Object>>(0L, pageNum, pageSize, new ArrayList<Map<String, Object>>()));
            }
            wrapper.in(Discussion::getCourseId, courseIds);
        }
        Page<Discussion> page = discussionMapper.selectPage(new Page<Discussion>(pageNum, pageSize), wrapper);
        List<Map<String, Object>> records = toRecords(page.getRecords());
        return ApiResult.ok(new PageResult<Map<String, Object>>(page.getTotal(), pageNum, pageSize, records));
    }

    @GetMapping
    public ApiResult<List<Map<String, Object>>> list(@RequestParam(required = false) Long courseId) {
        LambdaQueryWrapper<Discussion> wrapper = new LambdaQueryWrapper<Discussion>().orderByAsc(Discussion::getCreatedAt);
        if (courseId != null) wrapper.eq(Discussion::getCourseId, courseId);
        User current = scopeService.currentUser();
        if (current != null && "TEACHER".equals(current.getRole())) {
            List<Long> courseIds = scopeService.teacherCourseIds(current.getId());
            if (courseIds.isEmpty()) {
                return ApiResult.ok(new ArrayList<Map<String, Object>>());
            }
            wrapper.in(Discussion::getCourseId, courseIds);
        }
        return ApiResult.ok(toRecords(discussionMapper.selectList(wrapper)));
    }

    @PostMapping
    public ApiResult<Void> add(@RequestBody Discussion item) {
        item.setUserId(StpUtil.getLoginIdAsLong());
        item.setStatus(item.getParentId() == null ? "OPEN" : "REPLIED");
        item.setCreatedAt(LocalDateTime.now());
        discussionMapper.insert(item);
        if (item.getParentId() != null) {
            Discussion parent = discussionMapper.selectById(item.getParentId());
            if (parent != null) {
                parent.setStatus("REPLIED");
                discussionMapper.updateById(parent);
            }
        }
        return ApiResult.ok();
    }

    @PostMapping("/{id}/close")
    public ApiResult<Void> close(@PathVariable Long id) {
        Discussion item = discussionMapper.selectById(id);
        item.setStatus("CLOSED");
        discussionMapper.updateById(item);
        return ApiResult.ok();
    }

    @DeleteMapping("/{id}")
    public ApiResult<Void> delete(@PathVariable Long id) {
        discussionMapper.deleteById(id);
        return ApiResult.ok();
    }

    private String effectiveStatus(Discussion item) {
        if (item.getParentId() != null) {
            return "REPLIED";
        }
        Long replyCount = discussionMapper.selectCount(new LambdaQueryWrapper<Discussion>().eq(Discussion::getParentId, item.getId()));
        if (replyCount != null && replyCount > 0) {
            return "REPLIED";
        }
        return item.getStatus();
    }

    private List<Map<String, Object>> toRecords(List<Discussion> rows) {
        List<Map<String, Object>> records = new ArrayList<Map<String, Object>>();
        for (Discussion item : rows) {
            Map<String, Object> map = lookupService.names(item.getCourseId(), item.getUserId(), null, item.getStatus());
            map.put("id", item.getId());
            map.put("userId", item.getUserId());
            map.put("userName", lookupService.userName(item.getUserId()));
            map.put("userAvatar", lookupService.avatar(item.getUserId()));
            map.put("courseId", item.getCourseId());
            map.put("parentId", item.getParentId());
            Discussion parent = item.getParentId() == null ? null : discussionMapper.selectById(item.getParentId());
            map.put("parentContent", parent == null ? "" : parent.getContent());
            map.put("content", item.getContent());
            String status = effectiveStatus(item);
            map.put("status", status);
            map.put("statusText", StatusText.of(status));
            map.put("createdAt", item.getCreatedAt());
            records.add(map);
        }
        return records;
    }
}
