-- 在线学习平台数据库初始化脚本
-- 适用技术栈：JDK8、Maven、Spring Boot、MyBatis Plus、Sa-Token、MySQL
-- 模拟数据日期范围：2026-05-01 00:00:00 至 2026-06-03 23:59:59
-- 平台用户表：保存管理员、教师、学习者账号信息，密码按需求使用明文
CREATE TABLE sys_user (
id BIGINT PRIMARY KEY AUTO_INCREMENT,
username VARCHAR(50) NOT NULL UNIQUE,
password VARCHAR(100) NOT NULL,
real_name VARCHAR(50) NOT NULL,
role VARCHAR(20) NOT NULL,
phone VARCHAR(20),
email VARCHAR(100),
avatar VARCHAR(255),
status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
created_at DATETIME NOT NULL
);
-- 课程分类表：用于课程筛选和统计
CREATE TABLE course_category (
id BIGINT PRIMARY KEY AUTO_INCREMENT,
name VARCHAR(50) NOT NULL,
description VARCHAR(255),
sort INT DEFAULT 0
);
-- 课程表：保存课程基础信息、封面、视频和文档地址
CREATE TABLE course (
id BIGINT PRIMARY KEY AUTO_INCREMENT,
title VARCHAR(100) NOT NULL,
category_id BIGINT NOT NULL,
teacher_id BIGINT NOT NULL,
level VARCHAR(20) NOT NULL,
status VARCHAR(20) NOT NULL,
cover VARCHAR(255),
video_url VARCHAR(255),
doc_url VARCHAR(255),
description TEXT,
objective TEXT,
outline TEXT,
hot_score INT DEFAULT 0,
enroll_count INT DEFAULT 0,
duration_minutes INT DEFAULT 0,
created_at DATETIME NOT NULL,
updated_at DATETIME NOT NULL,
,
);
-- 课程资料表：保存课程视频、文档等资料上传路径
CREATE TABLE course_material (
id BIGINT PRIMARY KEY AUTO_INCREMENT,
course_id BIGINT NOT NULL,
type VARCHAR(20) NOT NULL,
name VARCHAR(100) NOT NULL,
file_url VARCHAR(255) NOT NULL,
sort INT DEFAULT 0,
created_at DATETIME NOT NULL,
);
-- 选课学习记录表：记录学习进度、学习时长和最近学习时间
CREATE TABLE enrollment (
id BIGINT PRIMARY KEY AUTO_INCREMENT,
student_id BIGINT NOT NULL,
course_id BIGINT NOT NULL,
progress DECIMAL(5,2) NOT NULL DEFAULT 0,
learn_minutes INT NOT NULL DEFAULT 0,
status VARCHAR(20) NOT NULL,
last_study_time DATETIME NOT NULL,
created_at DATETIME NOT NULL,
UNIQUE(student_id, course_id)
);
-- 课程讨论表：保存课程问答和教师回复
CREATE TABLE discussion (
id BIGINT PRIMARY KEY AUTO_INCREMENT,
course_id BIGINT NOT NULL,
user_id BIGINT NOT NULL,
parent_id BIGINT DEFAULT NULL,
content TEXT NOT NULL,
status VARCHAR(20) NOT NULL DEFAULT 'OPEN',
created_at DATETIME NOT NULL,
);
-- 学习笔记表：学习者记录和查看课程笔记
CREATE TABLE study_note (
id BIGINT PRIMARY KEY AUTO_INCREMENT,
student_id BIGINT NOT NULL,
course_id BIGINT NOT NULL,
content TEXT NOT NULL,
created_at DATETIME NOT NULL,
);
-- 作业表：教师发布课程作业
CREATE TABLE assignment (
id BIGINT PRIMARY KEY AUTO_INCREMENT,
course_id BIGINT NOT NULL,
title VARCHAR(100) NOT NULL,
description TEXT,
total_score DECIMAL(6,2) NOT NULL,
due_time DATETIME NOT NULL,
created_at DATETIME NOT NULL
);
-- 作业提交表：学习者提交作业，教师批改并给分
CREATE TABLE assignment_submission (
id BIGINT PRIMARY KEY AUTO_INCREMENT,
assignment_id BIGINT NOT NULL,
student_id BIGINT NOT NULL,
answer TEXT,
file_url VARCHAR(255),
score DECIMAL(6,2),
status VARCHAR(20) NOT NULL,
comment VARCHAR(255),
submitted_at DATETIME NOT NULL,
graded_at DATETIME
);
-- 测试题表：支持客观题自动批改和主观题人工批改
CREATE TABLE exam (
id BIGINT PRIMARY KEY AUTO_INCREMENT,
course_id BIGINT NOT NULL,
title VARCHAR(100) NOT NULL,
type VARCHAR(20) NOT NULL,
question TEXT NOT NULL,
answer TEXT,
total_score DECIMAL(6,2) NOT NULL,
created_at DATETIME NOT NULL
);
-- 测试记录表：学习者测试提交和成绩记录
CREATE TABLE exam_record (
id BIGINT PRIMARY KEY AUTO_INCREMENT,
exam_id BIGINT NOT NULL,
student_id BIGINT NOT NULL,
answer TEXT NOT NULL,
score DECIMAL(6,2),
status VARCHAR(20) NOT NULL,
submitted_at DATETIME NOT NULL
);
-- 课程评价表：学习者对课程评分和留言
CREATE TABLE course_review (
id BIGINT PRIMARY KEY AUTO_INCREMENT,
course_id BIGINT NOT NULL,
student_id BIGINT NOT NULL,
rating INT NOT NULL,
content VARCHAR(500),
created_at DATETIME NOT NULL
);
-- 学生分组表：教师按教学需要维护学习小组
CREATE TABLE student_group (
id BIGINT PRIMARY KEY AUTO_INCREMENT,
teacher_id BIGINT NOT NULL,
course_id BIGINT NOT NULL,
name VARCHAR(100) NOT NULL,
description VARCHAR(255),
created_at DATETIME NOT NULL
);
-- 学生分组成员表：保存分组与学习者关系
CREATE TABLE student_group_member (
id BIGINT PRIMARY KEY AUTO_INCREMENT,
group_id BIGINT NOT NULL,
student_id BIGINT NOT NULL,
UNIQUE(group_id, student_id)
);