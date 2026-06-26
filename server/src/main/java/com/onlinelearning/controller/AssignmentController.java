package com.onlinelearning.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.onlinelearning.common.ApiResult;
import com.onlinelearning.common.PageResult;
import com.onlinelearning.common.StatusText;
import com.onlinelearning.entity.Assignment;
import com.onlinelearning.entity.Submission;
import com.onlinelearning.entity.User;
import com.onlinelearning.mapper.AssignmentMapper;
import com.onlinelearning.mapper.SubmissionMapper;
import com.onlinelearning.service.LookupService;
import com.onlinelearning.service.ScopeService;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/api/assignments")
public class AssignmentController {
    @Resource private AssignmentMapper assignmentMapper;
    @Resource private SubmissionMapper submissionMapper;
    @Resource private LookupService lookupService;
    @Resource private ScopeService scopeService;

    @GetMapping("/page")
    public ApiResult<PageResult<Map<String, Object>>> page(@RequestParam(defaultValue = "1") Long pageNum,
                                                           @RequestParam(defaultValue = "10") Long pageSize,
                                                           @RequestParam(required = false) Long courseId,
                                                           @RequestParam(required = false) String keyword) {
        LambdaQueryWrapper<Assignment> wrapper = new LambdaQueryWrapper<Assignment>().orderByDesc(Assignment::getCreatedAt);
        if (courseId != null) wrapper.eq(Assignment::getCourseId, courseId);
        if (StringUtils.hasText(keyword)) wrapper.and(w -> w.like(Assignment::getTitle, keyword).or().like(Assignment::getDescription, keyword));
        User current = scopeService.currentUser();
        if (current != null && "TEACHER".equals(current.getRole())) {
            List<Long> courseIds = scopeService.teacherCourseIds(current.getId());
            if (courseIds.isEmpty()) return ApiResult.ok(new PageResult<Map<String, Object>>(0L, pageNum, pageSize, new ArrayList<Map<String, Object>>()));
            wrapper.in(Assignment::getCourseId, courseIds);
        }
        Page<Assignment> page = assignmentMapper.selectPage(new Page<Assignment>(pageNum, pageSize), wrapper);
        List<Map<String, Object>> records = new ArrayList<Map<String, Object>>();
        for (Assignment item : page.getRecords()) {
            Map<String, Object> map = new HashMap<String, Object>();
            map.put("id", item.getId());
            map.put("courseId", item.getCourseId());
            map.put("courseName", lookupService.courseName(item.getCourseId()));
            map.put("title", item.getTitle());
            map.put("description", item.getDescription());
            map.put("totalScore", item.getTotalScore());
            map.put("dueTime", item.getDueTime());
            map.put("createdAt", item.getCreatedAt());
            if (current != null && "STUDENT".equals(current.getRole())) {
                Submission submission = submissionMapper.selectOne(new LambdaQueryWrapper<Submission>()
                        .eq(Submission::getAssignmentId, item.getId())
                        .eq(Submission::getStudentId, current.getId())
                        .orderByDesc(Submission::getSubmittedAt)
                        .last("limit 1"));
                map.put("submitted", submission != null);
                map.put("submitStatusText", submission == null ? "未提交" : StatusText.of(submission.getStatus()));
                map.put("submitAnswer", submission == null ? "" : submission.getAnswer());
                map.put("submitFileUrl", submission == null ? "" : submission.getFileUrl());
                map.put("submitScore", submission == null ? null : submission.getScore());
                map.put("submitComment", submission == null ? "" : submission.getComment());
                map.put("submittedAt", submission == null ? "" : submission.getSubmittedAt());
            }
            records.add(map);
        }
        return ApiResult.ok(new PageResult<Map<String, Object>>(page.getTotal(), pageNum, pageSize, records));
    }

    @PostMapping
    public ApiResult<Void> add(@RequestBody Assignment item) {
        item.setCreatedAt(LocalDateTime.now());
        assignmentMapper.insert(item);
        return ApiResult.ok();
    }

    @PutMapping
    public ApiResult<Void> update(@RequestBody Assignment item) {
        assignmentMapper.updateById(item);
        return ApiResult.ok();
    }

    @DeleteMapping("/{id}")
    public ApiResult<Void> delete(@PathVariable Long id) {
        assignmentMapper.deleteById(id);
        return ApiResult.ok();
    }
}
