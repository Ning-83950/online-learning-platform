-- 在线学习平台数据库初始化脚本
-- 适用技术栈：JDK8、Maven、Spring Boot、MyBatis Plus、Sa-Token、MySQL
-- 模拟数据日期范围：2026-05-01 00:00:00 至 2026-06-03 23:59:59

DROP DATABASE IF EXISTS online_learning_platform;
CREATE DATABASE online_learning_platform DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
USE online_learning_platform;

-- 平台用户表：保存管理员、教师、学习者账号信息，密码按需求使用明文
CREATE TABLE sys_user (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
  username VARCHAR(50) NOT NULL UNIQUE COMMENT '登录用户名',
  password VARCHAR(100) NOT NULL COMMENT '明文密码',
  real_name VARCHAR(50) NOT NULL COMMENT '真实姓名',
  role VARCHAR(20) NOT NULL COMMENT '角色：ADMIN管理员，TEACHER教师，STUDENT学习者',
  phone VARCHAR(20) COMMENT '手机号',
  email VARCHAR(100) COMMENT '邮箱',
  avatar VARCHAR(255) COMMENT '头像上传路径',
  status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' COMMENT '状态：ACTIVE正常，BANNED已封禁',
  created_at DATETIME NOT NULL COMMENT '创建时间'
) COMMENT='平台用户表';

-- 课程分类表：用于课程筛选和统计
CREATE TABLE course_category (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
  name VARCHAR(50) NOT NULL COMMENT '分类名称',
  description VARCHAR(255) COMMENT '分类说明',
  sort INT DEFAULT 0 COMMENT '排序值'
) COMMENT='课程分类表';

-- 课程表：保存课程基础信息、封面、视频和文档地址
CREATE TABLE course (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
  title VARCHAR(100) NOT NULL COMMENT '课程标题',
  category_id BIGINT NOT NULL COMMENT '课程分类ID',
  teacher_id BIGINT NOT NULL COMMENT '授课教师ID',
  level VARCHAR(20) NOT NULL COMMENT '难度：BEGINNER入门，INTERMEDIATE进阶，ADVANCED高级',
  status VARCHAR(20) NOT NULL COMMENT '审核状态：PENDING待审核，APPROVED已通过，REJECTED已驳回',
  cover VARCHAR(255) COMMENT '课程封面上传路径',
  video_url VARCHAR(255) COMMENT '课程主视频上传路径',
  doc_url VARCHAR(255) COMMENT '课程文档上传路径',
  description TEXT COMMENT '课程介绍',
  objective TEXT COMMENT '学习目标',
  outline TEXT COMMENT '课程大纲',
  hot_score INT DEFAULT 0 COMMENT '热门分值',
  enroll_count INT DEFAULT 0 COMMENT '学习人数',
  duration_minutes INT DEFAULT 0 COMMENT '课程时长分钟',
  created_at DATETIME NOT NULL COMMENT '创建时间',
  updated_at DATETIME NOT NULL COMMENT '更新时间',
  INDEX idx_course_category(category_id),
  INDEX idx_course_teacher(teacher_id)
) COMMENT='课程表';

-- 课程资料表：保存课程视频、文档等资料上传路径
CREATE TABLE course_material (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
  course_id BIGINT NOT NULL COMMENT '课程ID',
  type VARCHAR(20) NOT NULL COMMENT '资料类型：VIDEO视频，DOCUMENT文档',
  name VARCHAR(100) NOT NULL COMMENT '资料名称',
  file_url VARCHAR(255) NOT NULL COMMENT '资料上传路径',
  sort INT DEFAULT 0 COMMENT '排序值',
  created_at DATETIME NOT NULL COMMENT '创建时间',
  INDEX idx_material_course(course_id)
) COMMENT='课程资料表';

-- 选课学习记录表：记录学习进度、学习时长和最近学习时间
CREATE TABLE enrollment (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
  student_id BIGINT NOT NULL COMMENT '学习者ID',
  course_id BIGINT NOT NULL COMMENT '课程ID',
  progress DECIMAL(5,2) NOT NULL DEFAULT 0 COMMENT '学习进度百分比',
  learn_minutes INT NOT NULL DEFAULT 0 COMMENT '累计学习分钟',
  status VARCHAR(20) NOT NULL COMMENT '学习状态：LEARNING学习中，FINISHED已完成',
  last_study_time DATETIME NOT NULL COMMENT '最近学习时间',
  created_at DATETIME NOT NULL COMMENT '创建时间',
  UNIQUE KEY uk_student_course(student_id, course_id)
) COMMENT='选课学习记录表';

-- 课程讨论表：保存课程问答和教师回复
CREATE TABLE discussion (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
  course_id BIGINT NOT NULL COMMENT '课程ID',
  user_id BIGINT NOT NULL COMMENT '发言用户ID',
  parent_id BIGINT DEFAULT NULL COMMENT '父级讨论ID，空表示主问题',
  content TEXT NOT NULL COMMENT '讨论内容',
  status VARCHAR(20) NOT NULL DEFAULT 'OPEN' COMMENT '状态：OPEN未回复，REPLIED已回复，CLOSED已关闭',
  created_at DATETIME NOT NULL COMMENT '创建时间',
  INDEX idx_discussion_course(course_id)
) COMMENT='课程讨论表';

-- 学习笔记表：学习者记录和查看课程笔记
CREATE TABLE study_note (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
  student_id BIGINT NOT NULL COMMENT '学习者ID',
  course_id BIGINT NOT NULL COMMENT '课程ID',
  content TEXT NOT NULL COMMENT '笔记内容',
  created_at DATETIME NOT NULL COMMENT '创建时间',
  INDEX idx_note_student(student_id)
) COMMENT='学习笔记表';

-- 作业表：教师发布课程作业
CREATE TABLE assignment (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
  course_id BIGINT NOT NULL COMMENT '课程ID',
  title VARCHAR(100) NOT NULL COMMENT '作业标题',
  description TEXT COMMENT '作业说明',
  total_score DECIMAL(6,2) NOT NULL COMMENT '满分',
  due_time DATETIME NOT NULL COMMENT '截止时间',
  created_at DATETIME NOT NULL COMMENT '创建时间'
) COMMENT='课程作业表';

-- 作业提交表：学习者提交作业，教师批改并给分
CREATE TABLE assignment_submission (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
  assignment_id BIGINT NOT NULL COMMENT '作业ID',
  student_id BIGINT NOT NULL COMMENT '学习者ID',
  answer TEXT COMMENT '文字答案',
  file_url VARCHAR(255) COMMENT '附件上传路径',
  score DECIMAL(6,2) COMMENT '得分',
  status VARCHAR(20) NOT NULL COMMENT '状态：SUBMITTED已提交，GRADED已批改',
  comment VARCHAR(255) COMMENT '批改评语',
  submitted_at DATETIME NOT NULL COMMENT '提交时间',
  graded_at DATETIME COMMENT '批改时间'
) COMMENT='作业提交表';

-- 测试题表：支持客观题自动批改和主观题人工批改
CREATE TABLE exam (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
  course_id BIGINT NOT NULL COMMENT '课程ID',
  title VARCHAR(100) NOT NULL COMMENT '测试标题',
  type VARCHAR(20) NOT NULL COMMENT '题型：OBJECTIVE客观题，SUBJECTIVE主观题',
  question TEXT NOT NULL COMMENT '题目内容',
  answer TEXT COMMENT '标准答案',
  total_score DECIMAL(6,2) NOT NULL COMMENT '满分',
  created_at DATETIME NOT NULL COMMENT '创建时间'
) COMMENT='测试题表';

-- 测试记录表：学习者测试提交和成绩记录
CREATE TABLE exam_record (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
  exam_id BIGINT NOT NULL COMMENT '测试题ID',
  student_id BIGINT NOT NULL COMMENT '学习者ID',
  answer TEXT NOT NULL COMMENT '学习者答案',
  score DECIMAL(6,2) COMMENT '得分',
  status VARCHAR(20) NOT NULL COMMENT '状态：SUBMITTED已提交，GRADED已批改',
  submitted_at DATETIME NOT NULL COMMENT '提交时间'
) COMMENT='测试记录表';

-- 课程评价表：学习者对课程评分和留言
CREATE TABLE course_review (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
  course_id BIGINT NOT NULL COMMENT '课程ID',
  student_id BIGINT NOT NULL COMMENT '学习者ID',
  rating INT NOT NULL COMMENT '评分，1到5',
  content VARCHAR(500) COMMENT '评价内容',
  created_at DATETIME NOT NULL COMMENT '创建时间'
) COMMENT='课程评价表';

-- 学生分组表：教师按教学需要维护学习小组
CREATE TABLE student_group (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
  teacher_id BIGINT NOT NULL COMMENT '教师ID',
  course_id BIGINT NOT NULL COMMENT '课程ID',
  name VARCHAR(100) NOT NULL COMMENT '分组名称',
  description VARCHAR(255) COMMENT '分组说明',
  created_at DATETIME NOT NULL COMMENT '创建时间'
) COMMENT='学生分组表';

-- 学生分组成员表：保存分组与学习者关系
CREATE TABLE student_group_member (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
  group_id BIGINT NOT NULL COMMENT '分组ID',
  student_id BIGINT NOT NULL COMMENT '学习者ID',
  UNIQUE KEY uk_group_student(group_id, student_id)
) COMMENT='学生分组成员表';

INSERT INTO sys_user (id, username, password, real_name, role, phone, email, avatar, status, created_at) VALUES
(1, 'admin', '123456', '平台管理员', 'ADMIN', '13800000001', 'admin@online.edu', '/upload/avatars/admin.jpg', 'ACTIVE', '2026-05-01 09:00:00'),
(2, 'teacher_li', '123456', '李明老师', 'TEACHER', '13800000002', 'li@online.edu', '/upload/avatars/teacher-li.jpg', 'ACTIVE', '2026-05-02 10:20:00'),
(3, 'teacher_wang', '123456', '王芳老师', 'TEACHER', '13800000003', 'wang@online.edu', '/upload/avatars/teacher-wang.jpg', 'ACTIVE', '2026-05-03 14:30:00'),
(4, 'student_chen', '123456', '陈雨同学', 'STUDENT', '13800000004', 'chen@online.edu', '/upload/avatars/student-chen.jpg', 'ACTIVE', '2026-05-04 08:40:00'),
(5, 'student_zhao', '123456', '赵晨同学', 'STUDENT', '13800000005', 'zhao@online.edu', '/upload/avatars/student-zhao.jpg', 'ACTIVE', '2026-05-06 16:10:00'),
(6, 'student_test', '123456', '测试学习者', 'STUDENT', '13800000006', 'student@online.edu', '/upload/avatars/student-chen.jpg', 'BANNED', '2026-05-08 11:05:00');

INSERT INTO course_category (id, name, description, sort) VALUES
(1, '编程开发', 'Java、Web、工程实践等开发课程', 1),
(2, '数据分析', '数据分析、可视化、业务洞察课程', 2),
(3, '语言学习', '英语表达、职场沟通和写作课程', 3),
(4, '学习方法', '在线学习规划、效率提升课程', 4);

INSERT INTO course (id, title, category_id, teacher_id, level, status, cover, video_url, doc_url, description, objective, outline, hot_score, enroll_count, duration_minutes, created_at, updated_at) VALUES
(1, 'Java Web 后端开发实战', 1, 2, 'INTERMEDIATE', 'APPROVED', '/upload/courses/course-java.jpg', '/upload/materials/java-web-intro.mp4', '/upload/materials/java-web-guide.pdf', '围绕 Spring Boot、MVC 分层、接口开发和数据库访问完成后端项目。', '掌握 Java Web 项目结构、接口设计、分页查询和业务闭环。', '第1章 项目结构；第2章 Spring Boot 接口；第3章 MyBatis Plus；第4章 实战项目。', 96, 2, 620, '2026-05-05 09:30:00', '2026-06-01 18:20:00'),
(2, '前端页面与交互设计', 1, 3, 'BEGINNER', 'APPROVED', '/upload/courses/course-web.jpg', '/upload/materials/frontend-ui.mp4', '/upload/materials/frontend-ui.pdf', '学习 HTML、CSS、JavaScript 和后台管理页面交互。', '能独立完成简洁大气、可用性良好的系统页面。', '布局基础；表单组件；列表分页；文件上传；响应式适配。', 88, 1, 480, '2026-05-07 10:00:00', '2026-05-30 16:45:00'),
(3, '业务数据分析入门', 2, 2, 'BEGINNER', 'APPROVED', '/upload/courses/course-data.jpg', '/upload/materials/data-analysis.mp4', '/upload/materials/data-analysis.pdf', '通过真实业务指标理解用户活跃、课程热度和学习时长。', '掌握指标口径、图表呈现和业务分析报告撰写。', '指标定义；数据清洗；图表分析；结论汇报。', 82, 1, 360, '2026-05-10 13:20:00', '2026-06-02 10:15:00'),
(4, '职场英语表达训练', 3, 3, 'INTERMEDIATE', 'PENDING', '/upload/courses/course-english.jpg', '/upload/materials/business-english.mp4', '/upload/materials/business-english.pdf', '面向职场会议、邮件和汇报场景的英语表达训练。', '提升会议沟通、邮件表达和项目汇报能力。', '会议表达；邮件写作；汇报演练；综合复盘。', 69, 0, 300, '2026-05-18 15:00:00', '2026-06-03 12:30:00'),
(5, '高效在线学习方法', 4, 2, 'BEGINNER', 'REJECTED', '/upload/courses/course-online.jpg', '/upload/materials/learning-plan.mp4', '/upload/materials/learning-plan.pdf', '介绍目标拆解、进度追踪和复盘方法。', '建立稳定的在线学习计划和记录习惯。', '目标设定；学习计划；进度追踪；复盘优化。', 57, 0, 180, '2026-05-20 09:15:00', '2026-05-28 17:40:00');

INSERT INTO course_material (course_id, type, name, file_url, sort, created_at) VALUES
(1, 'VIDEO', 'Spring Boot3 入门到项目实战公开课', '/upload/materials/java-web-intro.mp4', 1, '2026-05-06 10:00:00'),
(1, 'DOCUMENT', 'Java Web 实战讲义', '/upload/materials/java-web-guide.pdf', 2, '2026-05-06 10:05:00'),
(2, 'VIDEO', '前端 Web 页面开发公开课', '/upload/materials/frontend-ui.mp4', 1, '2026-05-08 11:00:00'),
(2, 'DOCUMENT', '前端页面交互讲义', '/upload/materials/frontend-ui.pdf', 2, '2026-05-08 11:05:00'),
(3, 'VIDEO', 'Python 数据分析入门公开课', '/upload/materials/data-analysis.mp4', 1, '2026-05-11 09:20:00'),
(3, 'DOCUMENT', '业务指标分析模板', '/upload/materials/data-analysis.pdf', 2, '2026-05-11 09:30:00'),
(4, 'VIDEO', '商务会议英语公开课', '/upload/materials/business-english.mp4', 1, '2026-05-19 14:10:00'),
(4, 'DOCUMENT', '职场英语表达讲义', '/upload/materials/business-english.pdf', 2, '2026-05-19 14:20:00'),
(5, 'VIDEO', 'Learning How to Learn 学习方法公开课', '/upload/materials/learning-plan.mp4', 1, '2026-05-21 10:00:00'),
(5, 'DOCUMENT', '学习计划模板文档', '/upload/materials/learning-plan.pdf', 2, '2026-05-21 10:10:00');

INSERT INTO enrollment (student_id, course_id, progress, learn_minutes, status, last_study_time, created_at) VALUES
(4, 1, 72.50, 180, 'LEARNING', '2026-06-01 19:30:00', '2026-05-20 09:00:00'),
(5, 1, 46.00, 95, 'LEARNING', '2026-05-31 20:10:00', '2026-05-21 10:20:00'),
(4, 2, 88.00, 210, 'LEARNING', '2026-06-02 21:00:00', '2026-05-22 10:40:00'),
(5, 3, 64.00, 150, 'LEARNING', '2026-06-03 18:00:00', '2026-05-24 09:40:00');

INSERT INTO discussion (course_id, user_id, parent_id, content, status, created_at) VALUES
(1, 4, NULL, '接口分页查询时如何同时支持课程名称搜索？', 'REPLIED', '2026-05-21 20:30:00'),
(1, 2, 1, '建议在服务层做关联查询或先定位课程ID，再构造分页条件。', 'REPLIED', '2026-05-22 09:10:00'),
(2, 5, NULL, '列表里的状态字段是否应该翻译成中文？', 'REPLIED', '2026-05-25 18:05:00'),
(2, 3, 3, '是的，后台系统应避免直接显示英文枚举。', 'REPLIED', '2026-05-26 10:40:00');

INSERT INTO study_note (student_id, course_id, content, created_at) VALUES
(4, 1, 'MVC 分层要保持 Controller、Service、Mapper 职责清楚。', '2026-05-24 21:10:00'),
(4, 2, '后台页面左侧菜单和右侧内容需要独立滚动。', '2026-05-27 19:45:00'),
(5, 3, '统计图表要先明确指标口径，再选择图表类型。', '2026-06-01 20:00:00');

INSERT INTO assignment (id, course_id, title, description, total_score, due_time, created_at) VALUES
(1, 1, '完成课程分页接口', '实现课程列表的分页、分类筛选、难度筛选和关键字搜索。', 100.00, '2026-06-03 23:00:00', '2026-05-20 10:00:00'),
(2, 2, '完成登录页面布局', '使用两栏布局完成登录、注册和快速角色登录入口。', 100.00, '2026-06-02 23:00:00', '2026-05-22 11:00:00'),
(3, 3, '输出课程热度分析', '根据课程学习人数和学习时长输出分析结论。', 100.00, '2026-06-03 23:00:00', '2026-05-24 09:20:00');

INSERT INTO assignment_submission (assignment_id, student_id, answer, file_url, score, status, comment, submitted_at, graded_at) VALUES
(1, 4, '已完成分页条件组合，并处理状态中文显示。', '/upload/submissions/java-page-answer.docx', 92.00, 'GRADED', '结构清晰，继续注意边界条件。', '2026-05-29 21:00:00', '2026-05-30 09:40:00'),
(2, 4, '登录页面已完成两栏布局和快速登录。', '/upload/submissions/login-layout.png', 95.00, 'GRADED', '页面主题贴合在线学习平台。', '2026-05-31 20:30:00', '2026-06-01 10:00:00'),
(3, 5, '已提交课程热度分析初稿。', '/upload/submissions/data-report.docx', NULL, 'SUBMITTED', NULL, '2026-06-03 16:20:00', NULL);

INSERT INTO exam (id, course_id, title, type, question, answer, total_score, created_at) VALUES
(1, 1, 'Spring Boot 启动类位置', 'OBJECTIVE', 'Spring Boot 启动类通常应放在什么包位置？A. 根包 B. 任意包 C. mapper包 D. static目录', 'A', 20.00, '2026-05-26 09:00:00'),
(2, 1, 'MVC 分层说明', 'SUBJECTIVE', '请说明 Controller、Service、Mapper 的职责边界。', 'Controller接收请求，Service处理业务，Mapper访问数据库。', 30.00, '2026-05-26 09:10:00'),
(3, 2, '列表状态显示', 'OBJECTIVE', '后台列表状态字段应如何展示？A. 英文枚举 B. 中文含义 C. 数据库ID D. 空值', 'B', 20.00, '2026-05-28 14:00:00');

INSERT INTO exam_record (exam_id, student_id, answer, score, status, submitted_at) VALUES
(1, 4, 'A', 20.00, 'GRADED', '2026-05-30 20:00:00'),
(2, 4, 'Controller 负责请求响应，Service 组织业务流程，Mapper 专注数据访问。', 28.00, 'GRADED', '2026-05-30 20:15:00'),
(3, 5, 'B', 20.00, 'GRADED', '2026-06-02 19:35:00');

INSERT INTO course_review (course_id, student_id, rating, content, created_at) VALUES
(1, 4, 5, '课程案例完整，接口和数据库设计讲得很清楚。', '2026-06-01 20:30:00'),
(2, 4, 5, '页面布局简洁，后台操作流程容易理解。', '2026-06-02 21:00:00'),
(3, 5, 4, '数据分析示例贴近真实业务，图表部分很实用。', '2026-06-03 18:20:00');

INSERT INTO student_group (id, teacher_id, course_id, name, description, created_at) VALUES
(1, 2, 1, 'Java 实战一组', '负责后端接口、分页和上传功能练习。', '2026-05-18 09:30:00'),
(2, 3, 2, '前端体验优化组', '负责页面布局、列表交互和状态中文显示。', '2026-05-19 10:00:00');

INSERT INTO student_group_member (group_id, student_id) VALUES
(1, 4),
(1, 5),
(2, 4);
