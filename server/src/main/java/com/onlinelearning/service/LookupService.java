package com.onlinelearning.service;

import com.onlinelearning.common.StatusText;
import com.onlinelearning.entity.*;
import com.onlinelearning.mapper.*;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.Map;

@Service
public class LookupService {
    @Resource private UserMapper userMapper;
    @Resource private CategoryMapper categoryMapper;
    @Resource private CourseMapper courseMapper;
    @Resource private AssignmentMapper assignmentMapper;
    @Resource private ExamMapper examMapper;
    @Resource private StudentGroupMapper groupMapper;

    public String userName(Long id) {
        User item = id == null ? null : userMapper.selectById(id);
        return item == null ? "" : item.getRealName();
    }

    public String avatar(Long id) {
        User item = id == null ? null : userMapper.selectById(id);
        return item == null ? "" : item.getAvatar();
    }

    public String categoryName(Long id) {
        Category item = id == null ? null : categoryMapper.selectById(id);
        return item == null ? "" : item.getName();
    }

    public String courseName(Long id) {
        Course item = id == null ? null : courseMapper.selectById(id);
        return item == null ? "" : item.getTitle();
    }

    public String assignmentTitle(Long id) {
        Assignment item = id == null ? null : assignmentMapper.selectById(id);
        return item == null ? "" : item.getTitle();
    }

    public String examTitle(Long id) {
        Exam item = id == null ? null : examMapper.selectById(id);
        return item == null ? "" : item.getTitle();
    }

    public String groupName(Long id) {
        StudentGroup item = id == null ? null : groupMapper.selectById(id);
        return item == null ? "" : item.getName();
    }

    public Map<String, Object> names(Long courseId, Long studentId, Long teacherId, String status) {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("courseName", courseName(courseId));
        map.put("studentName", userName(studentId));
        map.put("teacherName", userName(teacherId));
        map.put("studentAvatar", avatar(studentId));
        map.put("teacherAvatar", avatar(teacherId));
        map.put("statusText", StatusText.of(status));
        return map;
    }
}
