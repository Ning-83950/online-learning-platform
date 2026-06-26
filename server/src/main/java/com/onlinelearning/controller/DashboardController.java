package com.onlinelearning.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.onlinelearning.common.ApiResult;
import com.onlinelearning.entity.Course;
import com.onlinelearning.entity.User;
import com.onlinelearning.mapper.*;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {
    @Resource private UserMapper userMapper;
    @Resource private CourseMapper courseMapper;
    @Resource private EnrollmentMapper enrollmentMapper;
    @Resource private AssignmentMapper assignmentMapper;
    @Resource private SubmissionMapper submissionMapper;
    @Resource private ExamRecordMapper examRecordMapper;
    @Resource private CategoryMapper categoryMapper;

    @GetMapping("/stats")
    public ApiResult<Map<String, Object>> stats() {
        Map<String, Object> result = new HashMap<String, Object>();
        result.put("userCount", userMapper.selectCount(new LambdaQueryWrapper<User>()));
        result.put("studentCount", userMapper.selectCount(new LambdaQueryWrapper<User>().eq(User::getRole, "STUDENT")));
        result.put("teacherCount", userMapper.selectCount(new LambdaQueryWrapper<User>().eq(User::getRole, "TEACHER")));
        result.put("courseCount", courseMapper.selectCount(new LambdaQueryWrapper<Course>()));
        result.put("enrollmentCount", enrollmentMapper.selectCount(null));
        result.put("assignmentCount", assignmentMapper.selectCount(null));
        result.put("submittedCount", submissionMapper.selectCount(null));
        result.put("examRecordCount", examRecordMapper.selectCount(null));
        result.put("courseByCategory", courseByCategory());
        result.put("hotCourses", hotCourses());
        result.put("activityTrend", activityTrend());
        return ApiResult.ok(result);
    }

    private List<Map<String, Object>> courseByCategory() {
        List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();
        categoryMapper.selectList(null).forEach(category -> {
            Long count = courseMapper.selectCount(new LambdaQueryWrapper<Course>().eq(Course::getCategoryId, category.getId()));
            list.add(item(category.getName(), count));
        });
        return list;
    }

    private List<Map<String, Object>> hotCourses() {
        List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();
        courseMapper.selectList(new LambdaQueryWrapper<Course>().orderByDesc(Course::getEnrollCount).last("limit 6")).forEach(course -> {
            list.add(item(course.getTitle(), course.getEnrollCount()));
        });
        return list;
    }

    private List<Map<String, Object>> activityTrend() {
        TreeMap<LocalDate, Integer> counts = new TreeMap<LocalDate, Integer>();
        userMapper.selectList(null).forEach(user -> addActivity(counts, user.getCreatedAt()));
        enrollmentMapper.selectList(null).forEach(enrollment -> {
            addActivity(counts, enrollment.getCreatedAt());
            addActivity(counts, enrollment.getLastStudyTime());
        });
        submissionMapper.selectList(null).forEach(submission -> addActivity(counts, submission.getSubmittedAt()));
        examRecordMapper.selectList(null).forEach(record -> addActivity(counts, record.getSubmittedAt()));
        if (counts.isEmpty()) {
            return new ArrayList<Map<String, Object>>();
        }

        LocalDate end = counts.lastKey();
        LocalDate start = end.minusDays(6);
        List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();
        for (LocalDate day = start; !day.isAfter(end); day = day.plusDays(1)) {
            Integer count = counts.get(day);
            list.add(item(day.toString(), count == null ? 0 : count));
        }
        return list;
    }

    private void addActivity(Map<LocalDate, Integer> counts, LocalDateTime time) {
        if (time == null) {
            return;
        }
        LocalDate day = time.toLocalDate();
        counts.put(day, counts.containsKey(day) ? counts.get(day) + 1 : 1);
    }

    private Map<String, Object> item(String name, Object value) {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("name", name);
        map.put("value", value);
        return map;
    }
}
