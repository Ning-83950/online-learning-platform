package com.onlinelearning.service;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.onlinelearning.entity.Assignment;
import com.onlinelearning.entity.Course;
import com.onlinelearning.entity.Exam;
import com.onlinelearning.entity.User;
import com.onlinelearning.mapper.AssignmentMapper;
import com.onlinelearning.mapper.CourseMapper;
import com.onlinelearning.mapper.ExamMapper;
import com.onlinelearning.mapper.UserMapper;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ScopeService {
    @Resource private UserMapper userMapper;
    @Resource private CourseMapper courseMapper;
    @Resource private AssignmentMapper assignmentMapper;
    @Resource private ExamMapper examMapper;

    public User currentUser() {
        if (!StpUtil.isLogin()) {
            return null;
        }
        return userMapper.selectById(StpUtil.getLoginIdAsLong());
    }

    public List<Long> teacherCourseIds(Long teacherId) {
        return courseMapper.selectList(new LambdaQueryWrapper<Course>().eq(Course::getTeacherId, teacherId))
                .stream().map(Course::getId).collect(Collectors.toList());
    }

    public List<Long> assignmentIdsByCourses(List<Long> courseIds) {
        return assignmentMapper.selectList(new LambdaQueryWrapper<Assignment>().in(Assignment::getCourseId, courseIds))
                .stream().map(Assignment::getId).collect(Collectors.toList());
    }

    public List<Long> examIdsByCourses(List<Long> courseIds) {
        return examMapper.selectList(new LambdaQueryWrapper<Exam>().in(Exam::getCourseId, courseIds))
                .stream().map(Exam::getId).collect(Collectors.toList());
    }
}
