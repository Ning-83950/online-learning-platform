package com.onlinelearning.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.onlinelearning.common.ApiResult;
import com.onlinelearning.common.PageResult;
import com.onlinelearning.common.StatusText;
import com.onlinelearning.entity.Submission;
import com.onlinelearning.entity.User;
import com.onlinelearning.mapper.SubmissionMapper;
import com.onlinelearning.service.LookupService;
import com.onlinelearning.service.ScopeService;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/api/submissions")
public class SubmissionController {
    @Resource private SubmissionMapper submissionMapper;
    @Resource private LookupService lookupService;
    @Resource private ScopeService scopeService;

    @GetMapping("/page")
    public ApiResult<PageResult<Map<String, Object>>> page(@RequestParam(defaultValue = "1") Long pageNum,
                                                           @RequestParam(defaultValue = "10") Long pageSize,
                                                           @RequestParam(required = false) Long assignmentId,
                                                           @RequestParam(required = false) Long studentId,
                                                           @RequestParam(required = false) String status,
                                                           @RequestParam(required = false) String keyword) {
        LambdaQueryWrapper<Submission> wrapper = new LambdaQueryWrapper<Submission>().orderByDesc(Submission::getSubmittedAt);
        if (assignmentId != null) wrapper.eq(Submission::getAssignmentId, assignmentId);
        if (studentId != null) wrapper.eq(Submission::getStudentId, studentId);
        if (StringUtils.hasText(status)) wrapper.eq(Submission::getStatus, status);
        if (StringUtils.hasText(keyword)) wrapper.like(Submission::getAnswer, keyword);
        User current = scopeService.currentUser();
        if (current != null && "STUDENT".equals(current.getRole())) {
            wrapper.eq(Submission::getStudentId, current.getId());
        }
        if (current != null && "TEACHER".equals(current.getRole())) {
            List<Long> courseIds = scopeService.teacherCourseIds(current.getId());
            if (courseIds.isEmpty()) return ApiResult.ok(new PageResult<Map<String, Object>>(0L, pageNum, pageSize, new ArrayList<Map<String, Object>>()));
            List<Long> assignmentIds = scopeService.assignmentIdsByCourses(courseIds);
            if (assignmentIds.isEmpty()) return ApiResult.ok(new PageResult<Map<String, Object>>(0L, pageNum, pageSize, new ArrayList<Map<String, Object>>()));
            wrapper.in(Submission::getAssignmentId, assignmentIds);
        }
        Page<Submission> page = submissionMapper.selectPage(new Page<Submission>(pageNum, pageSize), wrapper);
        List<Map<String, Object>> records = new ArrayList<Map<String, Object>>();
        for (Submission item : page.getRecords()) {
            records.add(enrich(item));
        }
        return ApiResult.ok(new PageResult<Map<String, Object>>(page.getTotal(), pageNum, pageSize, records));
    }

    @PostMapping
    public ApiResult<Void> submit(@RequestBody Submission item) {
        item.setStudentId(StpUtil.getLoginIdAsLong());
        item.setStatus("SUBMITTED");
        item.setSubmittedAt(LocalDateTime.now());
        submissionMapper.insert(item);
        return ApiResult.ok();
    }

    @PutMapping("/grade")
    public ApiResult<Void> grade(@RequestBody Submission item) {
        Submission exists = submissionMapper.selectById(item.getId());
        exists.setScore(item.getScore());
        exists.setComment(item.getComment());
        exists.setStatus("GRADED");
        exists.setGradedAt(LocalDateTime.now());
        submissionMapper.updateById(exists);
        return ApiResult.ok();
    }

    @PutMapping
    public ApiResult<Void> update(@RequestBody Submission item) {
        return grade(item);
    }

    @DeleteMapping("/{id}")
    public ApiResult<Void> delete(@PathVariable Long id) {
        submissionMapper.deleteById(id);
        return ApiResult.ok();
    }

    private Map<String, Object> enrich(Submission item) {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("id", item.getId());
        map.put("assignmentId", item.getAssignmentId());
        map.put("assignmentTitle", lookupService.assignmentTitle(item.getAssignmentId()));
        map.put("studentId", item.getStudentId());
        map.put("studentName", lookupService.userName(item.getStudentId()));
        map.put("studentAvatar", lookupService.avatar(item.getStudentId()));
        map.put("answer", item.getAnswer());
        map.put("fileUrl", item.getFileUrl());
        map.put("score", item.getScore());
        map.put("status", item.getStatus());
        map.put("statusText", StatusText.of(item.getStatus()));
        map.put("comment", item.getComment());
        map.put("submittedAt", item.getSubmittedAt());
        map.put("gradedAt", item.getGradedAt());
        return map;
    }
}
