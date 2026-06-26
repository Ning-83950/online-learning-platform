package com.onlinelearning.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.onlinelearning.common.ApiResult;
import com.onlinelearning.common.PageResult;
import com.onlinelearning.common.StatusText;
import com.onlinelearning.entity.Course;
import com.onlinelearning.entity.Enrollment;
import com.onlinelearning.entity.User;
import com.onlinelearning.mapper.CourseMapper;
import com.onlinelearning.mapper.EnrollmentMapper;
import com.onlinelearning.mapper.UserMapper;
import com.onlinelearning.service.LookupService;
import com.onlinelearning.service.ScopeService;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.stream.Collectors;
import java.util.*;

@RestController
@RequestMapping("/api/enrollments")
public class EnrollmentController {
    @Resource private EnrollmentMapper enrollmentMapper;
    @Resource private CourseMapper courseMapper;
    @Resource private UserMapper userMapper;
    @Resource private LookupService lookupService;
    @Resource private ScopeService scopeService;

    @GetMapping("/page")
    public ApiResult<PageResult<Map<String, Object>>> page(@RequestParam(defaultValue = "1") Long pageNum,
                                                           @RequestParam(defaultValue = "10") Long pageSize,
                                                           @RequestParam(required = false) String keyword,
                                                           @RequestParam(required = false) Long courseId,
                                                           @RequestParam(required = false) Long studentId,
                                                           @RequestParam(required = false) String status) {
        LambdaQueryWrapper<Enrollment> wrapper = new LambdaQueryWrapper<Enrollment>().orderByDesc(Enrollment::getLastStudyTime);
        if (courseId != null) wrapper.eq(Enrollment::getCourseId, courseId);
        if (studentId != null) wrapper.eq(Enrollment::getStudentId, studentId);
        if (StringUtils.hasText(status)) wrapper.eq(Enrollment::getStatus, status);
        if (StringUtils.hasText(keyword)) {
            List<Long> courseIds = courseMapper.selectList(new LambdaQueryWrapper<Course>().like(Course::getTitle, keyword))
                    .stream().map(Course::getId).collect(Collectors.toList());
            List<Long> studentIds = userMapper.selectList(new LambdaQueryWrapper<User>().like(User::getRealName, keyword))
                    .stream().map(User::getId).collect(Collectors.toList());
            if (courseIds.isEmpty() && studentIds.isEmpty()) {
                return ApiResult.ok(new PageResult<Map<String, Object>>(0L, pageNum, pageSize, new ArrayList<Map<String, Object>>()));
            }
            wrapper.and(w -> {
                if (!courseIds.isEmpty()) w.in(Enrollment::getCourseId, courseIds);
                if (!courseIds.isEmpty() && !studentIds.isEmpty()) w.or();
                if (!studentIds.isEmpty()) w.in(Enrollment::getStudentId, studentIds);
            });
        }
        User current = currentUser();
        if (current != null && "STUDENT".equals(current.getRole())) {
            wrapper.eq(Enrollment::getStudentId, current.getId());
        }
        if (current != null && "TEACHER".equals(current.getRole())) {
            List<Long> courseIds = scopeService.teacherCourseIds(current.getId());
            if (courseIds.isEmpty()) return ApiResult.ok(new PageResult<Map<String, Object>>(0L, pageNum, pageSize, new ArrayList<Map<String, Object>>()));
            wrapper.in(Enrollment::getCourseId, courseIds);
        }
        Page<Enrollment> page = enrollmentMapper.selectPage(new Page<Enrollment>(pageNum, pageSize), wrapper);
        List<Map<String, Object>> records = new ArrayList<Map<String, Object>>();
        for (Enrollment item : page.getRecords()) {
            records.add(enrich(item));
        }
        return ApiResult.ok(new PageResult<Map<String, Object>>(page.getTotal(), pageNum, pageSize, records));
    }

    @PostMapping("/join/{courseId}")
    public ApiResult<Void> join(@PathVariable Long courseId) {
        Long studentId = StpUtil.getLoginIdAsLong();
        Long count = enrollmentMapper.selectCount(new LambdaQueryWrapper<Enrollment>()
                .eq(Enrollment::getStudentId, studentId).eq(Enrollment::getCourseId, courseId));
        if (count != null && count > 0) {
            return ApiResult.ok();
        }
        Enrollment item = new Enrollment();
        item.setStudentId(studentId);
        item.setCourseId(courseId);
        item.setProgress(BigDecimal.ZERO);
        item.setLearnMinutes(0);
        item.setStatus("LEARNING");
        item.setCreatedAt(LocalDateTime.now());
        item.setLastStudyTime(LocalDateTime.now());
        enrollmentMapper.insert(item);
        Course course = courseMapper.selectById(courseId);
        course.setEnrollCount(course.getEnrollCount() == null ? 1 : course.getEnrollCount() + 1);
        courseMapper.updateById(course);
        return ApiResult.ok();
    }

    @PutMapping("/progress")
    public ApiResult<Void> progress(@RequestBody Enrollment item) {
        Long studentId = StpUtil.getLoginIdAsLong();
        boolean created = false;
        Enrollment exists = enrollmentMapper.selectOne(new LambdaQueryWrapper<Enrollment>()
                .eq(Enrollment::getStudentId, studentId)
                .eq(Enrollment::getCourseId, item.getCourseId())
                .last("limit 1"));
        if (exists == null) {
            created = true;
            item.setStudentId(studentId);
            item.setCreatedAt(LocalDateTime.now());
            item.setStatus("LEARNING");
            if (item.getProgress() == null) {
                item.setProgress(BigDecimal.ZERO);
            }
            if (item.getLearnMinutes() == null) {
                item.setLearnMinutes(0);
            }
            exists = item;
        }
        if (item.getProgress() != null && (exists.getProgress() == null || item.getProgress().compareTo(exists.getProgress()) > 0)) {
            exists.setProgress(item.getProgress());
        }
        if (item.getLearnMinutes() != null && (exists.getLearnMinutes() == null || item.getLearnMinutes() > exists.getLearnMinutes())) {
            exists.setLearnMinutes(item.getLearnMinutes());
        }
        exists.setStatus(exists.getProgress() != null && exists.getProgress().compareTo(new BigDecimal("100")) >= 0 ? "FINISHED" : "LEARNING");
        exists.setLastStudyTime(LocalDateTime.now());
        if (exists.getId() == null) {
            enrollmentMapper.insert(exists);
        } else {
            enrollmentMapper.updateById(exists);
        }
        if (created) {
            Course course = courseMapper.selectById(item.getCourseId());
            if (course != null) {
                course.setEnrollCount(course.getEnrollCount() == null ? 1 : course.getEnrollCount() + 1);
                courseMapper.updateById(course);
            }
        }
        return ApiResult.ok();
    }

    @DeleteMapping("/{id}")
    public ApiResult<Void> delete(@PathVariable Long id) {
        enrollmentMapper.deleteById(id);
        return ApiResult.ok();
    }

    private Map<String, Object> enrich(Enrollment item) {
        Map<String, Object> map = lookupService.names(item.getCourseId(), item.getStudentId(), null, item.getStatus());
        map.put("id", item.getId());
        map.put("studentId", item.getStudentId());
        map.put("courseId", item.getCourseId());
        map.put("progress", item.getProgress());
        map.put("learnMinutes", item.getLearnMinutes());
        map.put("status", item.getStatus());
        map.put("lastStudyTime", item.getLastStudyTime());
        map.put("createdAt", item.getCreatedAt());
        return map;
    }

    private User currentUser() {
        if (!StpUtil.isLogin()) return null;
        return userMapper.selectById(StpUtil.getLoginIdAsLong());
    }
}
