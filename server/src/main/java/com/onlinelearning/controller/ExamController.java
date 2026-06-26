package com.onlinelearning.controller;

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
import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/api/exams")
public class ExamController {
    @Resource private ExamMapper examMapper;
    @Resource private ExamRecordMapper recordMapper;
    @Resource private LookupService lookupService;
    @Resource private ScopeService scopeService;

    @GetMapping("/page")
    public ApiResult<PageResult<Map<String, Object>>> page(@RequestParam(defaultValue = "1") Long pageNum,
                                                           @RequestParam(defaultValue = "10") Long pageSize,
                                                           @RequestParam(required = false) Long courseId,
                                                           @RequestParam(required = false) String type,
                                                           @RequestParam(required = false) String keyword) {
        LambdaQueryWrapper<Exam> wrapper = new LambdaQueryWrapper<Exam>().orderByDesc(Exam::getCreatedAt);
        if (courseId != null) wrapper.eq(Exam::getCourseId, courseId);
        if (StringUtils.hasText(type)) wrapper.eq(Exam::getType, type);
        if (StringUtils.hasText(keyword)) wrapper.and(w -> w.like(Exam::getTitle, keyword).or().like(Exam::getQuestion, keyword));
        User current = scopeService.currentUser();
        if (current != null && "TEACHER".equals(current.getRole())) {
            List<Long> courseIds = scopeService.teacherCourseIds(current.getId());
            if (courseIds.isEmpty()) return ApiResult.ok(new PageResult<Map<String, Object>>(0L, pageNum, pageSize, new ArrayList<Map<String, Object>>()));
            wrapper.in(Exam::getCourseId, courseIds);
        }
        Page<Exam> page = examMapper.selectPage(new Page<Exam>(pageNum, pageSize), wrapper);
        List<Map<String, Object>> records = new ArrayList<Map<String, Object>>();
        for (Exam item : page.getRecords()) {
            Map<String, Object> map = new HashMap<String, Object>();
            map.put("id", item.getId());
            map.put("courseId", item.getCourseId());
            map.put("courseName", lookupService.courseName(item.getCourseId()));
            map.put("title", item.getTitle());
            map.put("type", item.getType());
            map.put("typeText", StatusText.of(item.getType()));
            map.put("question", item.getQuestion());
            map.put("answer", item.getAnswer());
            map.put("totalScore", item.getTotalScore());
            map.put("createdAt", item.getCreatedAt());
            if (current != null && "STUDENT".equals(current.getRole())) {
                ExamRecord record = recordMapper.selectOne(new LambdaQueryWrapper<ExamRecord>()
                        .eq(ExamRecord::getExamId, item.getId())
                        .eq(ExamRecord::getStudentId, current.getId())
                        .orderByDesc(ExamRecord::getSubmittedAt)
                        .last("limit 1"));
                map.put("submitted", record != null);
                map.put("submitStatusText", record == null ? "未提交" : StatusText.of(record.getStatus()));
                map.put("submitAnswer", record == null ? "" : record.getAnswer());
                map.put("submitScore", record == null ? null : record.getScore());
                map.put("submittedAt", record == null ? "" : record.getSubmittedAt());
            }
            records.add(map);
        }
        return ApiResult.ok(new PageResult<Map<String, Object>>(page.getTotal(), pageNum, pageSize, records));
    }

    @PostMapping
    public ApiResult<Void> add(@RequestBody Exam item) {
        item.setCreatedAt(LocalDateTime.now());
        examMapper.insert(item);
        return ApiResult.ok();
    }

    @PutMapping
    public ApiResult<Void> update(@RequestBody Exam item) {
        examMapper.updateById(item);
        return ApiResult.ok();
    }

    @DeleteMapping("/{id}")
    public ApiResult<Void> delete(@PathVariable Long id) {
        examMapper.deleteById(id);
        return ApiResult.ok();
    }
}
