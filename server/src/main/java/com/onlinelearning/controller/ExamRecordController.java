package com.onlinelearning.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.onlinelearning.common.ApiResult;
import com.onlinelearning.common.PageResult;
import com.onlinelearning.common.StatusText;
import com.onlinelearning.entity.Exam;
import com.onlinelearning.entity.ExamRecord;
import com.onlinelearning.entity.User;
import com.onlinelearning.mapper.ExamMapper;
import com.onlinelearning.mapper.ExamRecordMapper;
import com.onlinelearning.service.LookupService;
import com.onlinelearning.service.ScopeService;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/api/exam-records")
public class ExamRecordController {
    @Resource private ExamRecordMapper recordMapper;
    @Resource private ExamMapper examMapper;
    @Resource private LookupService lookupService;
    @Resource private ScopeService scopeService;

    @GetMapping("/page")
    public ApiResult<PageResult<Map<String, Object>>> page(@RequestParam(defaultValue = "1") Long pageNum,
                                                           @RequestParam(defaultValue = "10") Long pageSize,
                                                           @RequestParam(required = false) Long examId,
                                                           @RequestParam(required = false) Long studentId,
                                                           @RequestParam(required = false) String status,
                                                           @RequestParam(required = false) String keyword) {
        LambdaQueryWrapper<ExamRecord> wrapper = new LambdaQueryWrapper<ExamRecord>().orderByDesc(ExamRecord::getSubmittedAt);
        if (examId != null) wrapper.eq(ExamRecord::getExamId, examId);
        if (studentId != null) wrapper.eq(ExamRecord::getStudentId, studentId);
        if (StringUtils.hasText(status)) wrapper.eq(ExamRecord::getStatus, status);
        if (StringUtils.hasText(keyword)) wrapper.like(ExamRecord::getAnswer, keyword);
        User current = scopeService.currentUser();
        if (current != null && "STUDENT".equals(current.getRole())) {
            wrapper.eq(ExamRecord::getStudentId, current.getId());
        }
        if (current != null && "TEACHER".equals(current.getRole())) {
            List<Long> courseIds = scopeService.teacherCourseIds(current.getId());
            if (courseIds.isEmpty()) return ApiResult.ok(new PageResult<Map<String, Object>>(0L, pageNum, pageSize, new ArrayList<Map<String, Object>>()));
            List<Long> examIds = scopeService.examIdsByCourses(courseIds);
            if (examIds.isEmpty()) return ApiResult.ok(new PageResult<Map<String, Object>>(0L, pageNum, pageSize, new ArrayList<Map<String, Object>>()));
            wrapper.in(ExamRecord::getExamId, examIds);
        }
        Page<ExamRecord> page = recordMapper.selectPage(new Page<ExamRecord>(pageNum, pageSize), wrapper);
        List<Map<String, Object>> records = new ArrayList<Map<String, Object>>();
        for (ExamRecord item : page.getRecords()) {
            records.add(enrich(item));
        }
        return ApiResult.ok(new PageResult<Map<String, Object>>(page.getTotal(), pageNum, pageSize, records));
    }

    @PostMapping
    public ApiResult<Void> submit(@RequestBody ExamRecord item) {
        Exam exam = examMapper.selectById(item.getExamId());
        item.setStudentId(StpUtil.getLoginIdAsLong());
        item.setSubmittedAt(LocalDateTime.now());
        if (exam != null && "OBJECTIVE".equals(exam.getType())) {
            boolean correct = String.valueOf(exam.getAnswer()).trim().equalsIgnoreCase(String.valueOf(item.getAnswer()).trim());
            item.setScore(correct ? exam.getTotalScore() : BigDecimal.ZERO);
            item.setStatus("GRADED");
        } else {
            item.setStatus("SUBMITTED");
        }
        recordMapper.insert(item);
        return ApiResult.ok();
    }

    @PutMapping("/grade")
    public ApiResult<Void> grade(@RequestBody ExamRecord item) {
        ExamRecord exists = recordMapper.selectById(item.getId());
        exists.setScore(item.getScore());
        exists.setStatus("GRADED");
        recordMapper.updateById(exists);
        return ApiResult.ok();
    }

    @PutMapping
    public ApiResult<Void> update(@RequestBody ExamRecord item) {
        return grade(item);
    }

    @DeleteMapping("/{id}")
    public ApiResult<Void> delete(@PathVariable Long id) {
        recordMapper.deleteById(id);
        return ApiResult.ok();
    }

    private Map<String, Object> enrich(ExamRecord item) {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("id", item.getId());
        map.put("examId", item.getExamId());
        map.put("examTitle", lookupService.examTitle(item.getExamId()));
        map.put("studentId", item.getStudentId());
        map.put("studentName", lookupService.userName(item.getStudentId()));
        map.put("studentAvatar", lookupService.avatar(item.getStudentId()));
        map.put("answer", item.getAnswer());
        map.put("score", item.getScore());
        map.put("status", item.getStatus());
        map.put("statusText", StatusText.of(item.getStatus()));
        map.put("submittedAt", item.getSubmittedAt());
        return map;
    }
}
