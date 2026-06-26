package com.onlinelearning.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.onlinelearning.common.ApiResult;
import com.onlinelearning.entity.Category;
import com.onlinelearning.entity.Course;
import com.onlinelearning.entity.Enrollment;
import com.onlinelearning.entity.User;
import com.onlinelearning.mapper.CategoryMapper;
import com.onlinelearning.mapper.CourseMapper;
import com.onlinelearning.mapper.EnrollmentMapper;
import com.onlinelearning.mapper.UserMapper;
import com.onlinelearning.service.ScopeService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/options")
public class OptionController {
    @Resource private UserMapper userMapper;
    @Resource private CategoryMapper categoryMapper;
    @Resource private CourseMapper courseMapper;
    @Resource private EnrollmentMapper enrollmentMapper;
    @Resource private ScopeService scopeService;

    @GetMapping("/users")
    public ApiResult<List<User>> users(@RequestParam(required = false) String role) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<User>()
                .eq(User::getStatus, "ACTIVE")
                .orderByDesc(User::getCreatedAt);
        if (role != null && role.length() > 0) {
            wrapper.eq(User::getRole, role);
        }
        User current = scopeService.currentUser();
        if (current != null && "TEACHER".equals(current.getRole())) {
            if ("TEACHER".equals(role)) {
                wrapper.eq(User::getId, current.getId());
            }
            if ("STUDENT".equals(role)) {
                List<Long> courseIds = scopeService.teacherCourseIds(current.getId());
                if (courseIds.isEmpty()) return ApiResult.ok(new ArrayList<User>());
                List<Enrollment> enrollments = enrollmentMapper.selectList(new LambdaQueryWrapper<Enrollment>().in(Enrollment::getCourseId, courseIds));
                Set<Long> studentIds = enrollments.stream().map(Enrollment::getStudentId).collect(Collectors.toCollection(HashSet::new));
                if (studentIds.isEmpty()) return ApiResult.ok(new ArrayList<User>());
                wrapper.in(User::getId, studentIds);
            }
        }
        return ApiResult.ok(userMapper.selectList(wrapper));
    }

    @GetMapping("/categories")
    public ApiResult<List<Category>> categories() {
        return ApiResult.ok(categoryMapper.selectList(new LambdaQueryWrapper<Category>().orderByAsc(Category::getSort)));
    }

    @GetMapping("/courses")
    public ApiResult<List<Course>> courses(@RequestParam(required = false) Long teacherId) {
        LambdaQueryWrapper<Course> wrapper = new LambdaQueryWrapper<Course>().orderByDesc(Course::getCreatedAt);
        if (teacherId != null) {
            wrapper.eq(Course::getTeacherId, teacherId);
        }
        User current = scopeService.currentUser();
        if (current != null && "TEACHER".equals(current.getRole())) {
            wrapper.eq(Course::getTeacherId, current.getId());
        }
        if (current != null && "STUDENT".equals(current.getRole())) {
            wrapper.eq(Course::getStatus, "APPROVED");
        }
        return ApiResult.ok(courseMapper.selectList(wrapper));
    }
}
