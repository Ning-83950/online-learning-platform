package com.onlinelearning.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.onlinelearning.common.ApiResult;
import com.onlinelearning.common.PageResult;
import com.onlinelearning.common.StatusText;
import com.onlinelearning.entity.Assignment;
import com.onlinelearning.entity.Course;
import com.onlinelearning.entity.Enrollment;
import com.onlinelearning.entity.Exam;
import com.onlinelearning.entity.ExamRecord;
import com.onlinelearning.entity.StudentGroup;
import com.onlinelearning.entity.StudentGroupMember;
import com.onlinelearning.entity.Submission;
import com.onlinelearning.entity.User;
import com.onlinelearning.mapper.AssignmentMapper;
import com.onlinelearning.mapper.CourseMapper;
import com.onlinelearning.mapper.EnrollmentMapper;
import com.onlinelearning.mapper.ExamMapper;
import com.onlinelearning.mapper.ExamRecordMapper;
import com.onlinelearning.mapper.StudentGroupMapper;
import com.onlinelearning.mapper.StudentGroupMemberMapper;
import com.onlinelearning.mapper.SubmissionMapper;
import com.onlinelearning.mapper.UserMapper;
import com.onlinelearning.service.LookupService;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/api/courses")
public class CourseController {
    @Resource private CourseMapper courseMapper;
    @Resource private UserMapper userMapper;
    @Resource private EnrollmentMapper enrollmentMapper;
    @Resource private AssignmentMapper assignmentMapper;
    @Resource private SubmissionMapper submissionMapper;
    @Resource private ExamMapper examMapper;
    @Resource private ExamRecordMapper examRecordMapper;
    @Resource private StudentGroupMapper groupMapper;
    @Resource private StudentGroupMemberMapper groupMemberMapper;
    @Resource private LookupService lookupService;

    @GetMapping("/page")
    public ApiResult<PageResult<Map<String, Object>>> page(@RequestParam(defaultValue = "1") Long pageNum,
                                                           @RequestParam(defaultValue = "10") Long pageSize,
                                                           @RequestParam(required = false) String keyword,
                                                           @RequestParam(required = false) Long categoryId,
                                                           @RequestParam(required = false) String level,
                                                           @RequestParam(required = false) String status) {
        LambdaQueryWrapper<Course> wrapper = new LambdaQueryWrapper<Course>().orderByDesc(Course::getCreatedAt);
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(Course::getTitle, keyword).or().like(Course::getDescription, keyword));
        }
        if (categoryId != null) wrapper.eq(Course::getCategoryId, categoryId);
        if (StringUtils.hasText(level)) wrapper.eq(Course::getLevel, level);
        if (StringUtils.hasText(status)) wrapper.eq(Course::getStatus, status);
        User current = currentUser();
        if (current != null && "TEACHER".equals(current.getRole())) {
            wrapper.eq(Course::getTeacherId, current.getId());
        }
        if (current != null && "STUDENT".equals(current.getRole())) {
            wrapper.eq(Course::getStatus, "APPROVED");
        }
        Page<Course> page = courseMapper.selectPage(new Page<Course>(pageNum, pageSize), wrapper);
        List<Map<String, Object>> records = new ArrayList<Map<String, Object>>();
        for (Course item : page.getRecords()) {
            records.add(enrich(item));
        }
        return ApiResult.ok(new PageResult<Map<String, Object>>(page.getTotal(), pageNum, pageSize, records));
    }

    @GetMapping("/{id}")
    public ApiResult<Map<String, Object>> detail(@PathVariable Long id) {
        return ApiResult.ok(enrich(courseMapper.selectById(id)));
    }

    @GetMapping("/{id}/teacher-detail")
    public ApiResult<Map<String, Object>> teacherDetail(@PathVariable Long id) {
        Course course = courseMapper.selectById(id);
        User current = currentUser();
        if (current != null && "TEACHER".equals(current.getRole()) && (course == null || !current.getId().equals(course.getTeacherId()))) {
            throw new IllegalArgumentException("无权查看该课程详情");
        }
        Map<String, Object> result = new HashMap<String, Object>();
        result.put("course", enrich(course));
        if (course == null) {
            result.put("students", new ArrayList<Map<String, Object>>());
            return ApiResult.ok(result);
        }

        List<Assignment> assignments = assignmentMapper.selectList(new LambdaQueryWrapper<Assignment>().eq(Assignment::getCourseId, id));
        List<Exam> exams = examMapper.selectList(new LambdaQueryWrapper<Exam>().eq(Exam::getCourseId, id));
        List<Enrollment> enrollments = enrollmentMapper.selectList(new LambdaQueryWrapper<Enrollment>().eq(Enrollment::getCourseId, id));
        List<StudentGroup> groups = groupMapper.selectList(new LambdaQueryWrapper<StudentGroup>().eq(StudentGroup::getCourseId, id));
        Map<Long, List<String>> studentGroups = new HashMap<Long, List<String>>();
        for (StudentGroup group : groups) {
            List<StudentGroupMember> members = groupMemberMapper.selectList(new LambdaQueryWrapper<StudentGroupMember>().eq(StudentGroupMember::getGroupId, group.getId()));
            for (StudentGroupMember member : members) {
                studentGroups.computeIfAbsent(member.getStudentId(), k -> new ArrayList<String>()).add(group.getName());
            }
        }

        BigDecimal totalPossible = BigDecimal.ZERO;
        for (Assignment assignment : assignments) {
            if (assignment.getTotalScore() != null) totalPossible = totalPossible.add(assignment.getTotalScore());
        }
        for (Exam exam : exams) {
            if (exam.getTotalScore() != null) totalPossible = totalPossible.add(exam.getTotalScore());
        }

        List<Map<String, Object>> students = new ArrayList<Map<String, Object>>();
        for (Enrollment enrollment : enrollments) {
            Long studentId = enrollment.getStudentId();
            BigDecimal assignmentScore = BigDecimal.ZERO;
            int submittedAssignments = 0;
            for (Assignment assignment : assignments) {
                Submission submission = latestSubmission(assignment.getId(), studentId);
                if (submission != null) {
                    submittedAssignments++;
                    if (submission.getScore() != null) assignmentScore = assignmentScore.add(submission.getScore());
                }
            }
            BigDecimal examScore = BigDecimal.ZERO;
            int submittedExams = 0;
            for (Exam exam : exams) {
                ExamRecord record = latestExamRecord(exam.getId(), studentId);
                if (record != null) {
                    submittedExams++;
                    if (record.getScore() != null) examScore = examScore.add(record.getScore());
                }
            }
            BigDecimal totalScore = assignmentScore.add(examScore);
            Map<String, Object> row = new HashMap<String, Object>();
            row.put("studentId", studentId);
            row.put("studentName", lookupService.userName(studentId));
            row.put("studentAvatar", lookupService.avatar(studentId));
            row.put("groupNames", String.join("、", studentGroups.getOrDefault(studentId, new ArrayList<String>())));
            row.put("progress", enrollment.getProgress());
            row.put("learnMinutes", enrollment.getLearnMinutes());
            row.put("studyStatusText", StatusText.of(enrollment.getStatus()));
            row.put("assignmentScore", assignmentScore);
            row.put("submittedAssignments", submittedAssignments + "/" + assignments.size());
            row.put("examScore", examScore);
            row.put("submittedExams", submittedExams + "/" + exams.size());
            row.put("totalScore", totalScore);
            row.put("totalPossible", totalPossible);
            row.put("lastStudyTime", enrollment.getLastStudyTime());
            students.add(row);
        }
        students.sort((a, b) -> ((BigDecimal) b.get("totalScore")).compareTo((BigDecimal) a.get("totalScore")));
        for (int i = 0; i < students.size(); i++) {
            students.get(i).put("rank", i + 1);
        }
        result.put("students", students);
        result.put("assignmentCount", assignments.size());
        result.put("examCount", exams.size());
        result.put("studentCount", students.size());
        result.put("totalPossible", totalPossible);
        return ApiResult.ok(result);
    }

    @PostMapping
    public ApiResult<Void> add(@RequestBody Course item) {
        User current = currentUser();
        if (current != null && "TEACHER".equals(current.getRole())) {
            item.setTeacherId(current.getId());
            item.setStatus("PENDING");
        }
        item.setCreatedAt(LocalDateTime.now());
        item.setUpdatedAt(LocalDateTime.now());
        courseMapper.insert(item);
        return ApiResult.ok();
    }

    @PutMapping
    public ApiResult<Void> update(@RequestBody Course item) {
        User current = currentUser();
        Course exists = courseMapper.selectById(item.getId());
        if (exists == null) {
            throw new IllegalArgumentException("课程不存在");
        }
        if (current != null && "TEACHER".equals(current.getRole())) {
            if (!current.getId().equals(exists.getTeacherId())) {
                throw new IllegalArgumentException("无权编辑该课程");
            }
            if ("PENDING".equals(exists.getStatus())) {
                throw new IllegalArgumentException("课程待审核时不能编辑");
            }
            item.setTeacherId(current.getId());
            item.setStatus("PENDING");
        }
        item.setUpdatedAt(LocalDateTime.now());
        courseMapper.updateById(item);
        return ApiResult.ok();
    }

    @PostMapping("/{id}/approve")
    public ApiResult<Void> approve(@PathVariable Long id) {
        requireAdmin();
        Course item = courseMapper.selectById(id);
        item.setStatus("APPROVED");
        item.setUpdatedAt(LocalDateTime.now());
        courseMapper.updateById(item);
        return ApiResult.ok();
    }

    @PostMapping("/{id}/reject")
    public ApiResult<Void> reject(@PathVariable Long id) {
        requireAdmin();
        Course item = courseMapper.selectById(id);
        item.setStatus("REJECTED");
        item.setUpdatedAt(LocalDateTime.now());
        courseMapper.updateById(item);
        return ApiResult.ok();
    }

    @DeleteMapping("/{id}")
    public ApiResult<Void> delete(@PathVariable Long id) {
        User current = currentUser();
        Course exists = courseMapper.selectById(id);
        if (exists == null) {
            throw new IllegalArgumentException("课程不存在");
        }
        if (current == null) {
            throw new IllegalArgumentException("请先登录");
        }
        if ("TEACHER".equals(current.getRole())) {
            if (!current.getId().equals(exists.getTeacherId())) {
                throw new IllegalArgumentException("无权删除该课程");
            }
        } else if (!"ADMIN".equals(current.getRole())) {
            throw new IllegalArgumentException("无权删除该课程");
        }
        courseMapper.deleteById(id);
        return ApiResult.ok();
    }

    private Map<String, Object> enrich(Course item) {
        Map<String, Object> map = new HashMap<String, Object>();
        if (item == null) return map;
        map.put("id", item.getId());
        map.put("title", item.getTitle());
        map.put("categoryId", item.getCategoryId());
        map.put("categoryName", lookupService.categoryName(item.getCategoryId()));
        map.put("teacherId", item.getTeacherId());
        map.put("teacherName", lookupService.userName(item.getTeacherId()));
        map.put("teacherAvatar", lookupService.avatar(item.getTeacherId()));
        map.put("level", item.getLevel());
        map.put("levelText", levelText(item.getLevel()));
        map.put("status", item.getStatus());
        map.put("statusText", StatusText.of(item.getStatus()));
        map.put("cover", item.getCover());
        map.put("videoUrl", item.getVideoUrl());
        map.put("docUrl", item.getDocUrl());
        map.put("description", item.getDescription());
        map.put("objective", item.getObjective());
        map.put("outline", item.getOutline());
        map.put("hotScore", item.getHotScore());
        map.put("enrollCount", item.getEnrollCount());
        map.put("durationMinutes", item.getDurationMinutes());
        map.put("createdAt", item.getCreatedAt());
        map.put("updatedAt", item.getUpdatedAt());
        return map;
    }

    private User currentUser() {
        if (!StpUtil.isLogin()) return null;
        return userMapper.selectById(StpUtil.getLoginIdAsLong());
    }

    private Submission latestSubmission(Long assignmentId, Long studentId) {
        return submissionMapper.selectOne(new LambdaQueryWrapper<Submission>()
                .eq(Submission::getAssignmentId, assignmentId)
                .eq(Submission::getStudentId, studentId)
                .orderByDesc(Submission::getSubmittedAt)
                .last("limit 1"));
    }

    private ExamRecord latestExamRecord(Long examId, Long studentId) {
        return examRecordMapper.selectOne(new LambdaQueryWrapper<ExamRecord>()
                .eq(ExamRecord::getExamId, examId)
                .eq(ExamRecord::getStudentId, studentId)
                .orderByDesc(ExamRecord::getSubmittedAt)
                .last("limit 1"));
    }

    private void requireAdmin() {
        User current = currentUser();
        if (current == null || !"ADMIN".equals(current.getRole())) {
            throw new IllegalArgumentException("只有管理员可以审核课程");
        }
    }

    private String levelText(String level) {
        if ("BEGINNER".equals(level)) return "入门";
        if ("INTERMEDIATE".equals(level)) return "进阶";
        if ("ADVANCED".equals(level)) return "高级";
        return level;
    }
}
