package com.onlinelearning.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.onlinelearning.common.ApiResult;
import com.onlinelearning.common.PageResult;
import com.onlinelearning.entity.Course;
import com.onlinelearning.entity.Enrollment;
import com.onlinelearning.entity.StudentGroup;
import com.onlinelearning.entity.StudentGroupMember;
import com.onlinelearning.entity.User;
import com.onlinelearning.mapper.CourseMapper;
import com.onlinelearning.mapper.EnrollmentMapper;
import com.onlinelearning.mapper.StudentGroupMapper;
import com.onlinelearning.mapper.StudentGroupMemberMapper;
import com.onlinelearning.mapper.UserMapper;
import com.onlinelearning.service.LookupService;
import com.onlinelearning.service.ScopeService;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/api/groups")
public class GroupController {
    @Resource private StudentGroupMapper groupMapper;
    @Resource private StudentGroupMemberMapper memberMapper;
    @Resource private CourseMapper courseMapper;
    @Resource private EnrollmentMapper enrollmentMapper;
    @Resource private UserMapper userMapper;
    @Resource private LookupService lookupService;
    @Resource private ScopeService scopeService;

    @GetMapping("/page")
    public ApiResult<PageResult<Map<String, Object>>> page(@RequestParam(defaultValue = "1") Long pageNum,
                                                           @RequestParam(defaultValue = "10") Long pageSize,
                                                           @RequestParam(required = false) String keyword,
                                                           @RequestParam(required = false) Long teacherId,
                                                           @RequestParam(required = false) Long courseId) {
        LambdaQueryWrapper<StudentGroup> wrapper = new LambdaQueryWrapper<StudentGroup>().orderByDesc(StudentGroup::getCreatedAt);
        if (StringUtils.hasText(keyword)) wrapper.and(w -> w.like(StudentGroup::getName, keyword).or().like(StudentGroup::getDescription, keyword));
        if (teacherId != null) wrapper.eq(StudentGroup::getTeacherId, teacherId);
        if (courseId != null) wrapper.eq(StudentGroup::getCourseId, courseId);
        User current = scopeService.currentUser();
        if (current != null && "TEACHER".equals(current.getRole())) {
            wrapper.eq(StudentGroup::getTeacherId, current.getId());
        }
        Page<StudentGroup> page = groupMapper.selectPage(new Page<StudentGroup>(pageNum, pageSize), wrapper);
        List<Map<String, Object>> records = new ArrayList<Map<String, Object>>();
        for (StudentGroup item : page.getRecords()) {
            Map<String, Object> map = new HashMap<String, Object>();
            map.put("id", item.getId());
            map.put("teacherId", item.getTeacherId());
            map.put("teacherName", lookupService.userName(item.getTeacherId()));
            map.put("teacherAvatar", lookupService.avatar(item.getTeacherId()));
            map.put("courseId", item.getCourseId());
            map.put("courseName", lookupService.courseName(item.getCourseId()));
            map.put("name", item.getName());
            map.put("description", item.getDescription());
            map.put("memberCount", memberMapper.selectCount(new LambdaQueryWrapper<StudentGroupMember>().eq(StudentGroupMember::getGroupId, item.getId())));
            map.put("createdAt", item.getCreatedAt());
            records.add(map);
        }
        return ApiResult.ok(new PageResult<Map<String, Object>>(page.getTotal(), pageNum, pageSize, records));
    }

    @PostMapping
    public ApiResult<Void> add(@RequestBody StudentGroup item) {
        User current = scopeService.currentUser();
        if (current != null && "TEACHER".equals(current.getRole())) {
            checkCourseOwner(item.getCourseId(), current);
            item.setTeacherId(current.getId());
        } else if (item.getTeacherId() == null) {
            item.setTeacherId(StpUtil.getLoginIdAsLong());
        }
        item.setCreatedAt(LocalDateTime.now());
        groupMapper.insert(item);
        return ApiResult.ok();
    }

    @PutMapping
    public ApiResult<Void> update(@RequestBody StudentGroup item) {
        User current = scopeService.currentUser();
        if (current != null && "TEACHER".equals(current.getRole())) {
            checkGroupOwner(item.getId());
            checkCourseOwner(item.getCourseId(), current);
            item.setTeacherId(current.getId());
        }
        groupMapper.updateById(item);
        return ApiResult.ok();
    }

    @DeleteMapping("/{id}")
    public ApiResult<Void> delete(@PathVariable Long id) {
        checkGroupOwner(id);
        memberMapper.delete(new LambdaQueryWrapper<StudentGroupMember>().eq(StudentGroupMember::getGroupId, id));
        groupMapper.deleteById(id);
        return ApiResult.ok();
    }

    @GetMapping("/{groupId}/members")
    public ApiResult<List<Map<String, Object>>> members(@PathVariable Long groupId) {
        checkGroupOwner(groupId);
        List<StudentGroupMember> members = memberMapper.selectList(new LambdaQueryWrapper<StudentGroupMember>().eq(StudentGroupMember::getGroupId, groupId));
        List<Map<String, Object>> result = new ArrayList<Map<String, Object>>();
        for (StudentGroupMember member : members) {
            Map<String, Object> map = new HashMap<String, Object>();
            map.put("id", member.getId());
            map.put("groupId", member.getGroupId());
            map.put("groupName", lookupService.groupName(member.getGroupId()));
            map.put("studentId", member.getStudentId());
            map.put("studentName", lookupService.userName(member.getStudentId()));
            map.put("studentAvatar", lookupService.avatar(member.getStudentId()));
            result.add(map);
        }
        return ApiResult.ok(result);
    }

    @GetMapping("/{groupId}/available-students")
    public ApiResult<List<Map<String, Object>>> availableStudents(@PathVariable Long groupId) {
        StudentGroup group = checkGroupOwner(groupId);
        if (group == null) {
            throw new IllegalArgumentException("分组不存在");
        }
        List<Enrollment> enrollments = enrollmentMapper.selectList(new LambdaQueryWrapper<Enrollment>()
                .eq(Enrollment::getCourseId, group.getCourseId())
                .orderByDesc(Enrollment::getLastStudyTime));
        List<Map<String, Object>> result = new ArrayList<Map<String, Object>>();
        for (Enrollment enrollment : enrollments) {
            User student = userMapper.selectById(enrollment.getStudentId());
            if (student == null || !"STUDENT".equals(student.getRole())) {
                continue;
            }
            Map<String, Object> map = new HashMap<String, Object>();
            map.put("studentId", student.getId());
            map.put("studentName", student.getRealName());
            map.put("studentAvatar", student.getAvatar());
            map.put("progress", enrollment.getProgress());
            map.put("learnMinutes", enrollment.getLearnMinutes());
            map.put("statusText", com.onlinelearning.common.StatusText.of(enrollment.getStatus()));
            result.add(map);
        }
        return ApiResult.ok(result);
    }

    @PostMapping("/{groupId}/members/{studentId}")
    public ApiResult<Void> addMember(@PathVariable Long groupId, @PathVariable Long studentId) {
        StudentGroup group = checkGroupOwner(groupId);
        if (group == null) {
            throw new IllegalArgumentException("分组不存在");
        }
        Long enrolled = enrollmentMapper.selectCount(new LambdaQueryWrapper<Enrollment>()
                .eq(Enrollment::getCourseId, group.getCourseId())
                .eq(Enrollment::getStudentId, studentId));
        if (enrolled == null || enrolled == 0) {
            throw new IllegalArgumentException("只能添加已参与该课程的学习者");
        }
        Long count = memberMapper.selectCount(new LambdaQueryWrapper<StudentGroupMember>().eq(StudentGroupMember::getGroupId, groupId).eq(StudentGroupMember::getStudentId, studentId));
        if (count == null || count == 0) {
            StudentGroupMember member = new StudentGroupMember();
            member.setGroupId(groupId);
            member.setStudentId(studentId);
            memberMapper.insert(member);
        }
        return ApiResult.ok();
    }

    @DeleteMapping("/members/{id}")
    public ApiResult<Void> deleteMember(@PathVariable Long id) {
        StudentGroupMember member = memberMapper.selectById(id);
        if (member != null) {
            checkGroupOwner(member.getGroupId());
        }
        memberMapper.deleteById(id);
        return ApiResult.ok();
    }

    private StudentGroup checkGroupOwner(Long groupId) {
        User current = scopeService.currentUser();
        if (current == null || !"TEACHER".equals(current.getRole())) {
            return groupMapper.selectById(groupId);
        }
        StudentGroup group = groupMapper.selectById(groupId);
        if (group == null || !current.getId().equals(group.getTeacherId())) {
            throw new IllegalArgumentException("无权操作该分组");
        }
        return group;
    }

    private void checkCourseOwner(Long courseId, User current) {
        Course course = courseMapper.selectById(courseId);
        if (course == null || current == null || !current.getId().equals(course.getTeacherId())) {
            throw new IllegalArgumentException("只能管理自己课程下的分组");
        }
    }
}
