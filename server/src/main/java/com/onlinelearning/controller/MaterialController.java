package com.onlinelearning.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.onlinelearning.common.ApiResult;
import com.onlinelearning.common.PageResult;
import com.onlinelearning.common.StatusText;
import com.onlinelearning.entity.CourseMaterial;
import com.onlinelearning.entity.User;
import com.onlinelearning.mapper.CourseMaterialMapper;
import com.onlinelearning.service.LookupService;
import com.onlinelearning.service.ScopeService;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/api/materials")
public class MaterialController {
    @Resource private CourseMaterialMapper materialMapper;
    @Resource private LookupService lookupService;
    @Resource private ScopeService scopeService;

    @GetMapping("/page")
    public ApiResult<PageResult<Map<String, Object>>> page(@RequestParam(defaultValue = "1") Long pageNum,
                                                           @RequestParam(defaultValue = "10") Long pageSize,
                                                           @RequestParam(required = false) String keyword,
                                                           @RequestParam(required = false) Long courseId,
                                                           @RequestParam(required = false) String type) {
        LambdaQueryWrapper<CourseMaterial> wrapper = new LambdaQueryWrapper<CourseMaterial>().orderByAsc(CourseMaterial::getSort);
        if (StringUtils.hasText(keyword)) wrapper.like(CourseMaterial::getName, keyword);
        if (courseId != null) wrapper.eq(CourseMaterial::getCourseId, courseId);
        if (StringUtils.hasText(type)) wrapper.eq(CourseMaterial::getType, type);
        User current = scopeService.currentUser();
        if (current != null && "TEACHER".equals(current.getRole())) {
            List<Long> courseIds = scopeService.teacherCourseIds(current.getId());
            if (courseIds.isEmpty()) return ApiResult.ok(new PageResult<Map<String, Object>>(0L, pageNum, pageSize, new ArrayList<Map<String, Object>>()));
            wrapper.in(CourseMaterial::getCourseId, courseIds);
        }
        Page<CourseMaterial> page = materialMapper.selectPage(new Page<CourseMaterial>(pageNum, pageSize), wrapper);
        List<Map<String, Object>> records = toRecords(page.getRecords());
        return ApiResult.ok(new PageResult<Map<String, Object>>(page.getTotal(), pageNum, pageSize, records));
    }

    @GetMapping
    public ApiResult<List<Map<String, Object>>> list(@RequestParam(required = false) Long courseId) {
        LambdaQueryWrapper<CourseMaterial> wrapper = new LambdaQueryWrapper<CourseMaterial>().orderByAsc(CourseMaterial::getSort);
        if (courseId != null) wrapper.eq(CourseMaterial::getCourseId, courseId);
        User current = scopeService.currentUser();
        if (current != null && "TEACHER".equals(current.getRole())) {
            List<Long> courseIds = scopeService.teacherCourseIds(current.getId());
            if (courseIds.isEmpty()) return ApiResult.ok(new ArrayList<Map<String, Object>>());
            wrapper.in(CourseMaterial::getCourseId, courseIds);
        }
        return ApiResult.ok(toRecords(materialMapper.selectList(wrapper)));
    }

    @PostMapping
    public ApiResult<Void> add(@RequestBody CourseMaterial item) {
        item.setCreatedAt(LocalDateTime.now());
        materialMapper.insert(item);
        return ApiResult.ok();
    }

    @PutMapping
    public ApiResult<Void> update(@RequestBody CourseMaterial item) {
        materialMapper.updateById(item);
        return ApiResult.ok();
    }

    @DeleteMapping("/{id}")
    public ApiResult<Void> delete(@PathVariable Long id) {
        materialMapper.deleteById(id);
        return ApiResult.ok();
    }

    private List<Map<String, Object>> toRecords(List<CourseMaterial> rows) {
        List<Map<String, Object>> records = new ArrayList<Map<String, Object>>();
        for (CourseMaterial item : rows) {
            Map<String, Object> map = new HashMap<String, Object>();
            map.put("id", item.getId());
            map.put("courseId", item.getCourseId());
            map.put("courseName", lookupService.courseName(item.getCourseId()));
            map.put("type", item.getType());
            map.put("typeText", StatusText.of(item.getType()));
            map.put("name", item.getName());
            map.put("fileUrl", item.getFileUrl());
            map.put("sort", item.getSort());
            map.put("createdAt", item.getCreatedAt());
            records.add(map);
        }
        return records;
    }
}
