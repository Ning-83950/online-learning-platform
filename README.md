# 🎓 在线学习平台

<p align="center">
  <img src="https://img.shields.io/badge/Spring%20Boot-2.7.18-brightgreen" alt="Spring Boot">
  <img src="https://img.shields.io/badge/MyBatis%20Plus-3.5.5-blue" alt="MyBatis Plus">
  <img src="https://img.shields.io/badge/Sa--Token-1.37.0-red" alt="Sa-Token">
  <img src="https://img.shields.io/badge/MySQL-8.0-orange" alt="MySQL">
  <img src="https://img.shields.io/badge/Thymeleaf-✅-green" alt="Thymeleaf">
  <img src="https://img.shields.io/badge/License-MIT-yellow" alt="License">
</p>

## 📖 项目简介

在线学习平台是一个功能完善的在线教育系统，支持管理员、教师、学生三种角色。教师可以创建课程、上传资料、布置作业、发布考试；学生可以选修课程、提交作业、参与讨论、记录笔记；系统提供数据看板，直观展示课程参与度与成绩分布。

---

## 🚀 核心功能

### 🧑‍🏫 教师端

| 功能 | 说明 |
|------|------|
| **课程管理** | 创建课程、设置分类、上传课程封面 |
| **资料管理** | 上传课件（视频/PDF），管理课程学习资料 |
| **作业管理** | 发布作业，设定截止时间，批改学生提交 |
| **考试管理** | 创建在线考试（选择题），自动判分，查看成绩统计 |
| **讨论区** | 发起课程讨论，回复学生问题 |
| **分组管理** | 创建学习小组，分配小组成员 |
| **学习记录** | 查看学生课程笔记 |

### 🧑‍🎓 学生端

| 功能 | 说明 |
|------|------|
| **课程浏览** | 按分类浏览课程，查看课程详情 |
| **选课/退课** | 自由选课，支持退课 |
| **学习资料** | 查看课件视频和 PDF 文档 |
| **提交作业** | 在线提交作业（文件/文本），查看批改结果 |
| **参加考试** | 在线答题，实时出分 |
| **学习笔记** | 在线记录学习笔记 |
| **讨论参与** | 参与课程讨论 |
| **课程评价** | 对已学课程进行评分和评价 |

### 👑 管理员端

| 功能 | 说明 |
|------|------|
| **用户管理** | 管理教师和学生账号 |
| **分类管理** | 课程分类的增删改查 |
| **数据看板** | 课程选课统计、考试通过率、学习活跃度等可视化图表 |

---

## 🛠️ 技术栈

### 后端

| 技术 | 版本 | 用途 |
|------|------|------|
| Spring Boot | 2.7.18 | 核心框架 |
| MyBatis Plus | 3.5.5 | ORM 持久层 |
| Sa-Token | 1.37.0 | 权限认证 + 会话管理 |
| MySQL | 8.0.33 | 关系型数据库 |
| Thymeleaf | — | 模板引擎 |
| Lombok | — | 简化代码 |

### 前端

| 技术 | 用途 |
|------|------|
| Thymeleaf 模板 | 服务端渲染页面 |
| ECharts | 数据看板可视化 |
| 原生 JavaScript | 页面交互逻辑 |

---

## 📂 项目目录结构

```
在线学习平台/
├── server/                                    # Spring Boot 后端
│   ├── src/main/java/com/onlinelearning/
│   │   ├── OnlineLearningApplication.java     # 启动类
│   │   ├── common/                            # 通用类
│   │   │   ├── ApiResult.java                 # 统一响应格式
│   │   │   ├── PageResult.java                # 分页响应
│   │   │   └── StatusText.java                # 状态码
│   │   ├── config/                            # 配置类
│   │   │   ├── WebConfig.java                 # Web 配置
│   │   │   ├── MybatisPlusConfig.java         # MyBatis Plus 配置
│   │   │   ├── SaTokenConfig.java             # Sa-Token 配置
│   │   │   └── GlobalExceptionHandler.java    # 全局异常处理
│   │   ├── controller/                        # 控制器（18 个）
│   │   │   ├── AuthController.java            # 登录注册
│   │   │   ├── CourseController.java          # 课程管理
│   │   │   ├── EnrollmentController.java      # 选课管理
│   │   │   ├── AssignmentController.java      # 作业管理
│   │   │   ├── ExamController.java            # 考试管理
│   │   │   ├── SubmissionController.java      # 提交管理
│   │   │   ├── DiscussionController.java      # 讨论管理
│   │   │   ├── NoteController.java            # 笔记管理
│   │   │   └── DashboardController.java       # 数据看板
│   │   ├── entity/                            # 实体类（14 个）
│   │   ├── mapper/                            # 数据访问层
│   │   └── service/                           # 业务逻辑层
│   ├── src/main/resources/
│   │   ├── application.yml                    # 主配置文件
│   │   ├── templates/                         # Thymeleaf 页面模板
│   │   │   ├── admin.html                     # 管理员后台
│   │   │   ├── teacher.html                   # 教师后台
│   │   │   ├── student.html                   # 学生端
│   │   │   ├── login.html                     # 登录页
│   │   │   └── register.html                  # 注册页
│   │   └── static/                            # 静态资源
│   └── pom.xml                                # Maven 依赖
├── sql/
│   └── init.sql                               # 数据库初始化脚本
└── upload/                                    # 上传文件目录
    ├── avatars/                               # 用户头像
    ├── courses/                               # 课程封面
    ├── materials/                             # 课程资料
    └── submissions/                           # 学生提交
```

---

## 💻 本地运行

### 环境要求

- **JDK 1.8**
- **Maven 3.6+**
- **MySQL 8.0**

### 步骤

#### 1. 创建数据库

```sql
CREATE DATABASE online_learning_platform DEFAULT CHARACTER SET utf8mb4;
```

#### 2. 导入初始数据

在 MySQL 中执行 `sql/init.sql`

#### 3. 修改配置

编辑 `server/src/main/resources/application.yml`：

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/online_learning_platform?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai
    username: root
    password: 你的密码

app:
  upload-root: D:/在线学习平台/upload   # 改为你本机的上传目录路径
```

#### 4. 启动项目

```bash
cd server
mvn spring-boot:run
```

#### 5. 访问系统

浏览器打开：`http://localhost:8080`

- **管理员后台**：`http://localhost:8080/admin`
- **教师后台**：`http://localhost:8080/teacher`
- **学生端**：`http://localhost:8080/student`
- **登录页**：`http://localhost:8080/login`

---

## 🏗️ 数据库表结构

| 表名 | 说明 |
|------|------|
| `user` | 用户表（管理员/教师/学生） |
| `category` | 课程分类表 |
| `course` | 课程表 |
| `course_material` | 课程资料表 |
| `enrollment` | 选课记录表 |
| `assignment` | 作业表 |
| `submission` | 作业提交表 |
| `exam` | 考试表 |
| `exam_record` | 考试记录表 |
| `discussion` | 讨论帖表 |
| `note` | 学习笔记表 |
| `review` | 课程评价表 |
| `student_group` | 学习小组表 |
| `student_group_member` | 小组成员表 |

---

## 📄 License

MIT License

---

> 💡 本项目为大学课程作业，如有问题欢迎提 Issue！
