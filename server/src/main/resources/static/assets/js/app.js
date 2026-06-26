const API_BASE = "/api";
let currentModule = "";
let currentPage = 1;
let currentSize = 10;
let editingId = null;

const state = {
  user: JSON.parse(localStorage.getItem("user") || "null"),
  options: { users: [], teachers: [], students: [], categories: [], courses: [] }
};

async function api(url, options = {}) {
  const headers = Object.assign({}, options.headers || {});
  headers.satoken = localStorage.getItem("satoken") || "";
  if (!(options.body instanceof FormData)) headers["Content-Type"] = "application/json";
  const response = await fetch(API_BASE + url, Object.assign({}, options, { headers }));
  const result = await response.json();
  if (response.status === 401 || result.code === 401) {
    handleLoginExpired();
    throw new Error("登录已过期");
  }
  return result;
}

function handleLoginExpired() {
  localStorage.removeItem("satoken");
  localStorage.removeItem("user");
  alert("登录已过期，请重新登录");
  location.href = "/login";
}

function guard() {
  if (!localStorage.getItem("satoken")) {
    alert("登录已过期，请重新登录");
    location.href = "/login";
    return;
  }
  const name = document.querySelector("[data-user-name]");
  if (name && state.user) name.textContent = state.user.realName + " · " + roleText(state.user.role);
}

function roleText(role) {
  return { ADMIN: "管理员", TEACHER: "教师", STUDENT: "学习者" }[role] || role;
}

function levelText(level) {
  return { BEGINNER: "入门", INTERMEDIATE: "进阶", ADVANCED: "高级" }[level] || level || "";
}

function statusText(status) {
  return {
    ACTIVE: "正常", BANNED: "已封禁", PENDING: "待审核", APPROVED: "已通过", REJECTED: "已驳回",
    LEARNING: "学习中", FINISHED: "已完成", SUBMITTED: "已提交", GRADED: "已批改", OPEN: "未回复", REPLIED: "已回复", CLOSED: "已关闭",
    OBJECTIVE: "客观题", SUBJECTIVE: "主观题", VIDEO: "视频", DOCUMENT: "文档"
  }[status] || status || "";
}

function fillSelect(name, rows, valueKey, labelKey, selected) {
  const select = document.querySelector(`[name=${name}]`);
  if (!select) return;
  select.innerHTML = `<option value="">请选择</option>` + rows.map(item => {
    const value = item[valueKey];
    const label = item[labelKey];
    return `<option value="${value}" ${String(value) === String(selected || "") ? "selected" : ""}>${label}</option>`;
  }).join("");
}

async function loadOptions() {
  const [categories, courses, teachers, students, users] = await Promise.all([
    api("/options/categories"),
    api("/options/courses"),
    api("/options/users?role=TEACHER"),
    api("/options/users?role=STUDENT"),
    api("/options/users")
  ]);
  state.options.categories = categories.data || [];
  state.options.courses = courses.data || [];
  state.options.teachers = teachers.data || [];
  state.options.students = students.data || [];
  state.options.users = users.data || [];
}

const moduleConfig = {
  dashboard: { title: "数据统计" },
  users: {
    title: "用户管理", endpoint: "/users",
    filters: [["keyword", "关键词"], ["role", "角色", [["", "全部角色"], ["ADMIN", "管理员"], ["TEACHER", "教师"], ["STUDENT", "学习者"]]], ["status", "状态", [["", "全部状态"], ["ACTIVE", "正常"], ["BANNED", "已封禁"]]]],
    columns: [["avatar", "头像", "image:avatar"], ["realName", "姓名"], ["username", "账号"], ["roleText", "角色"], ["phone", "手机号"], ["email", "邮箱"], ["statusText", "状态"], ["createdAt", "创建时间"]],
    fields: [["username", "用户名"], ["password", "密码"], ["realName", "姓名"], ["role", "角色", "select:roles"], ["phone", "手机号"], ["email", "邮箱"], ["avatar", "头像", "upload:avatars"], ["status", "状态", "select:userStatus"]],
    actions: ["edit", "ban", "enable", "delete"]
  },
  categories: {
    title: "课程分类", endpoint: "/categories",
    filters: [["keyword", "分类名称"]],
    columns: [["name", "分类名称"], ["description", "说明"], ["sort", "排序"]],
    fields: [["name", "分类名称"], ["description", "说明", "textarea"], ["sort", "排序"]]
  },
  courses: {
    title: "课程管理", endpoint: "/courses",
    filters: [["keyword", "课程名称"], ["categoryId", "课程分类", "select:categories"], ["level", "难度", [["", "全部难度"], ["BEGINNER", "入门"], ["INTERMEDIATE", "进阶"], ["ADVANCED", "高级"]]], ["status", "审核状态", [["", "全部状态"], ["PENDING", "待审核"], ["APPROVED", "已通过"], ["REJECTED", "已驳回"]]]],
    columns: [["cover", "封面", "image:thumb"], ["title", "课程名称"], ["categoryName", "分类"], ["teacherName", "授课教师"], ["levelText", "难度"], ["statusText", "审核状态"], ["enrollCount", "学习人数"], ["updatedAt", "更新时间"]],
    fields: [["title", "课程名称"], ["categoryId", "课程分类", "select:categories"], ["teacherId", "授课教师", "select:teachers"], ["level", "难度", "select:levels"], ["status", "审核状态", "select:courseStatus"], ["cover", "封面", "upload:courses"], ["videoUrl", "课程视频", "upload:materials"], ["docUrl", "课程文档", "upload:materials"], ["description", "课程介绍", "textarea"], ["objective", "学习目标", "textarea"], ["outline", "课程大纲", "textarea"], ["hotScore", "热门分值"], ["durationMinutes", "课程时长"]],
    actions: ["edit", "approve", "reject", "delete"]
  },
  materials: {
    title: "课程资料", endpoint: "/materials",
    filters: [["keyword", "资料名称"], ["courseId", "课程", "select:courses"], ["type", "资料类型", [["", "全部类型"], ["VIDEO", "视频"], ["DOCUMENT", "文档"]]]],
    columns: [["name", "资料名称"], ["courseName", "课程"], ["typeText", "类型"], ["fileUrl", "文件", "file"], ["sort", "排序"], ["createdAt", "创建时间"]],
    fields: [["courseId", "课程", "select:courses"], ["type", "资料类型", "select:materialTypes"], ["name", "资料名称"], ["fileUrl", "上传文件", "upload:materials"], ["sort", "排序"]]
  },
  enrollments: {
    title: "学习记录", endpoint: "/enrollments",
    filters: [["keyword", "课程或学生"], ["courseId", "课程", "select:courses"], ["studentId", "学习者", "select:students"], ["status", "学习状态", [["", "全部状态"], ["LEARNING", "学习中"], ["FINISHED", "已完成"]]]],
    columns: [["studentAvatar", "头像", "image:avatar"], ["studentName", "学习者"], ["courseName", "课程"], ["progress", "进度"], ["learnMinutes", "学习分钟"], ["statusText", "状态"], ["lastStudyTime", "最近学习"]]
  },
  notes: {
    title: "学习笔记", endpoint: "/notes",
    filters: [["keyword", "笔记内容"], ["courseId", "课程", "select:courses"]],
    columns: [["studentAvatar", "头像", "image:avatar"], ["studentName", "学习者"], ["courseName", "课程"], ["content", "笔记内容"], ["createdAt", "记录时间"]],
    fields: [["courseId", "课程", "select:courses"], ["content", "笔记内容", "textarea"]]
  },
  assignments: {
    title: "作业管理", endpoint: "/assignments",
    filters: [["keyword", "作业标题"], ["courseId", "课程", "select:courses"]],
    columns: [["title", "作业"], ["courseName", "课程"], ["totalScore", "满分"], ["dueTime", "截止时间"], ["createdAt", "创建时间"]],
    fields: [["courseId", "课程", "select:courses"], ["title", "作业标题"], ["description", "作业说明", "textarea"], ["totalScore", "满分"], ["dueTime", "截止时间"]]
  },
  submissions: {
    title: "作业提交", endpoint: "/submissions",
    filters: [["keyword", "答案内容"], ["status", "批改状态", [["", "全部状态"], ["SUBMITTED", "已提交"], ["GRADED", "已批改"]]]],
    columns: [["studentAvatar", "头像", "image:avatar"], ["studentName", "学习者"], ["assignmentTitle", "作业"], ["answer", "答案"], ["fileUrl", "附件", "file"], ["score", "得分"], ["statusText", "状态"], ["submittedAt", "提交时间"]],
    allowAdd: false,
    actions: ["grade"]
  },
  exams: {
    title: "测试管理", endpoint: "/exams",
    filters: [["keyword", "测试标题"], ["courseId", "课程", "select:courses"], ["type", "题型", [["", "全部题型"], ["OBJECTIVE", "客观题"], ["SUBJECTIVE", "主观题"]]]],
    columns: [["title", "测试"], ["courseName", "课程"], ["typeText", "题型"], ["question", "题目"], ["totalScore", "满分"], ["createdAt", "创建时间"]],
    fields: [["courseId", "课程", "select:courses"], ["title", "测试标题"], ["type", "题型", "select:examTypes"], ["question", "题目", "textarea"], ["answer", "标准答案", "textarea"], ["totalScore", "满分"]]
  },
  examRecords: {
    title: "测试成绩", endpoint: "/exam-records",
    filters: [["keyword", "答案内容"], ["status", "批改状态", [["", "全部状态"], ["SUBMITTED", "已提交"], ["GRADED", "已批改"]]]],
    columns: [["studentAvatar", "头像", "image:avatar"], ["studentName", "学习者"], ["examTitle", "测试"], ["answer", "答案"], ["score", "得分"], ["statusText", "状态"], ["submittedAt", "提交时间"]],
    allowAdd: false,
    actions: ["grade"]
  },
  discussions: {
    title: "教学互动", endpoint: "/discussions",
    filters: [["keyword", "讨论内容"], ["courseId", "课程", "select:courses"]],
    columns: [["userAvatar", "头像", "image:avatar"], ["userName", "用户"], ["courseName", "课程"], ["parentContent", "回复对象"], ["content", "内容"], ["statusText", "状态"], ["createdAt", "时间"]],
    fields: [["courseId", "课程", "select:courses"], ["content", "内容", "textarea"]],
    actions: ["reply", "delete"]
  },
  reviews: {
    title: "课程评价", endpoint: "/reviews",
    filters: [["keyword", "评价内容"], ["courseId", "课程", "select:courses"]],
    columns: [["studentAvatar", "头像", "image:avatar"], ["studentName", "学习者"], ["courseName", "课程"], ["rating", "评分"], ["content", "评价"], ["createdAt", "时间"]],
    fields: [["courseId", "课程", "select:courses"], ["rating", "评分"], ["content", "评价", "textarea"]]
  },
  groups: {
    title: "学生分组", endpoint: "/groups",
    filters: [["keyword", "分组名称"], ["courseId", "课程", "select:courses"]],
    columns: [["courseName", "课程"], ["name", "分组标签"], ["description", "说明"], ["memberCount", "成员数"], ["createdAt", "创建时间"]],
    fields: [["courseId", "所属课程", "select:courses"], ["name", "分组标签"], ["description", "说明", "textarea"]],
    actions: ["members", "edit", "delete"]
  }
};

const dictionaries = {
  roles: [["ADMIN", "管理员"], ["TEACHER", "教师"], ["STUDENT", "学习者"]],
  userStatus: [["ACTIVE", "正常"], ["BANNED", "已封禁"]],
  levels: [["BEGINNER", "入门"], ["INTERMEDIATE", "进阶"], ["ADVANCED", "高级"]],
  courseStatus: [["PENDING", "待审核"], ["APPROVED", "已通过"], ["REJECTED", "已驳回"]],
  materialTypes: [["VIDEO", "视频"], ["DOCUMENT", "文档"]],
  examTypes: [["OBJECTIVE", "客观题"], ["SUBJECTIVE", "主观题"]],
  gradeStatus: [["SUBMITTED", "已提交"], ["GRADED", "已批改"]]
};

async function initApp(defaultModule) {
  guard();
  await loadOptions();
  bindNav();
  openModule(defaultModule || "dashboard");
}

function bindNav() {
  document.querySelectorAll("[data-module]").forEach(btn => {
    btn.onclick = () => openModule(btn.dataset.module);
  });
  const logout = document.querySelector("[data-logout]");
  if (logout) logout.onclick = async () => {
    await api("/auth/logout", { method: "POST", body: "{}" });
    localStorage.clear();
    location.href = "/login";
  };
}

async function openModule(name) {
  currentModule = name;
  currentPage = 1;
  document.querySelectorAll("[data-module]").forEach(btn => btn.classList.toggle("active", btn.dataset.module === name));
  if (name === "dashboard") return renderDashboard();
  renderListShell();
  await loadList();
}

function renderListShell() {
  const cfg = moduleConfig[currentModule];
  const noStudentAdd = ["courses", "materials", "assignments", "submissions", "exams", "examRecords", "groups"].includes(currentModule);
  const canAdd = cfg.fields && cfg.allowAdd !== false && !(state.user && state.user.role === "STUDENT" && noStudentAdd);
  const root = document.querySelector("#content");
  root.innerHTML = `<div class="topbar"><h2 class="page-title">${cfg.title}</h2><div data-user-name></div></div>
    <div class="toolbar" id="toolbar"><div class="toolbar-fields">${renderFilters(cfg)}</div><div class="toolbar-actions"><button class="ghost-btn" data-search>查询</button>${canAdd ? '<button class="primary-btn" data-add>新增</button>' : ''}</div></div>
    <div class="table-wrap"><table><thead></thead><tbody></tbody></table></div>
    <div class="pagination"><button class="ghost-btn" data-prev>上一页</button><span data-page-info></span><button class="ghost-btn" data-next>下一页</button></div>`;
  if (state.user) document.querySelector("[data-user-name]").textContent = state.user.realName + " · " + roleText(state.user.role);
  fillFilterOptions();
  document.querySelector("[data-search]").onclick = () => { currentPage = 1; loadList(); };
  const add = document.querySelector("[data-add]");
  if (add) add.onclick = () => openForm();
  document.querySelector("[data-prev]").onclick = () => { if (currentPage > 1) { currentPage--; loadList(); } };
  document.querySelector("[data-next]").onclick = () => { currentPage++; loadList(); };
}

function renderFilters(cfg) {
  const filters = cfg.filters || [];
  return filters.map(f => {
    const name = f[0];
    const label = f[1];
    if (Array.isArray(f[2])) {
      return `<div class="toolbar-field"><label>${label}</label><select name="${name}">${f[2].map(o => `<option value="${o[0]}">${o[1]}</option>`).join("")}</select></div>`;
    }
    if (typeof f[2] === "string") {
      return `<div class="toolbar-field"><label>${label}</label><select name="${name}" data-dict="${f[2].replace("select:", "")}"><option value="">全部</option></select></div>`;
    }
    return `<div class="toolbar-field"><label>${label}</label><input name="${name}" placeholder="请输入${label}"></div>`;
  }).join("");
}

function fillFilterOptions() {
  document.querySelectorAll("[data-dict]").forEach(select => {
    const key = select.dataset.dict;
    let rows = [];
    if (key === "categories") rows = state.options.categories.map(x => [x.id, x.name]);
    if (key === "courses") rows = state.options.courses.map(x => [x.id, x.title]);
    if (key === "teachers") rows = state.options.teachers.map(x => [x.id, x.realName]);
    if (key === "students") rows = state.options.students.map(x => [x.id, x.realName]);
    select.innerHTML += rows.map(x => `<option value="${x[0]}">${x[1]}</option>`).join("");
  });
}

async function loadList() {
  const cfg = moduleConfig[currentModule];
  const columns = currentColumns(cfg);
  const params = new URLSearchParams({ pageNum: currentPage, pageSize: currentSize });
  document.querySelectorAll("#toolbar input,#toolbar select").forEach(el => {
    if (el.value) params.append(el.name, el.value);
  });
  const result = await api(`${cfg.endpoint}/page?${params.toString()}`);
  const page = result.data || { records: [], total: 0 };
  const thead = document.querySelector("thead");
  const tbody = document.querySelector("tbody");
  thead.innerHTML = `<tr>${columns.map(c => `<th>${c[1]}</th>`).join("")}<th>操作</th></tr>`;
  tbody.innerHTML = (page.records || []).map(row => `<tr>${columns.map(c => `<td>${formatCell(row, c)}</td>`).join("")}<td>${renderActions(row)}</td></tr>`).join("");
  document.querySelector("[data-page-info]").textContent = `第 ${currentPage} 页 / 共 ${page.total || 0} 条`;
  bindRowActions(page.records || []);
}

function currentColumns(cfg) {
  if (state.user && state.user.role === "STUDENT" && currentModule === "assignments") {
    return [...cfg.columns, ["submitStatusText", "提交状态"], ["submitInfo", "提交信息", "assignmentSubmitInfo"]];
  }
  if (state.user && state.user.role === "STUDENT" && currentModule === "exams") {
    return [...cfg.columns, ["submitStatusText", "提交状态"], ["submitInfo", "提交信息", "examSubmitInfo"]];
  }
  return cfg.columns;
}

function formatCell(row, column) {
  const key = column[0];
  const type = column[2] || "";
  const value = row[key];
  if (type.startsWith("image")) {
    return value ? `<img class="${type.includes("avatar") ? "avatar" : "thumb"}" src="${value}" alt="">` : "";
  }
  if (type === "file") {
    if (!value) return "";
    if (/\.(png|jpg|jpeg|gif|webp)$/i.test(value)) return `<img class="thumb" src="${value}" alt="">`;
    return `<a class="badge" href="${value}" target="_blank">查看文件</a>`;
  }
  if (key === "progress") return `${value || 0}%`;
  if (type === "assignmentSubmitInfo") return renderAssignmentSubmitStatus(row);
  if (type === "examSubmitInfo") return renderExamSubmitStatus(row);
  if (key === "statusText" || key.endsWith("Text")) return `<span class="badge">${value || ""}</span>`;
  return value == null ? "" : String(value);
}

function renderAssignmentSubmitStatus(row) {
  if (!row.submitted) return `<span class="muted">暂无提交</span>`;
  const file = row.submitFileUrl ? `<a class="badge" href="${row.submitFileUrl}" target="_blank">${fileName(row.submitFileUrl)}</a>` : "";
  const score = row.submitScore == null ? "" : `<span>得分：${row.submitScore}</span>`;
  const comment = row.submitComment ? `<span>评语：${row.submitComment}</span>` : "";
  return `<div class="submit-cell"><p>${row.submitAnswer || ""}</p><div>${file}${score}${comment}</div><span class="muted">${row.submittedAt || ""}</span></div>`;
}

function renderExamSubmitStatus(row) {
  if (!row.submitted) return `<span class="muted">暂无提交</span>`;
  const score = row.submitScore == null ? "" : `<span>得分：${row.submitScore}</span>`;
  return `<div class="submit-cell"><p>${row.submitAnswer || ""}</p><div>${score}</div><span class="muted">${row.submittedAt || ""}</span></div>`;
}

function renderActions(row) {
  const cfg = moduleConfig[currentModule];
  if (currentModule === "courses") {
    return renderCourseActions(row);
  }
  if (state.user && state.user.role === "STUDENT" && currentModule === "assignments") {
    return `<div class="actions"><button class="primary-btn" data-action="submitAssignment" data-id="${row.id}">${row.submitted ? "重新提交" : "提交作业"}</button></div>`;
  }
  if (state.user && state.user.role === "STUDENT" && currentModule === "exams") {
    return `<div class="actions"><button class="primary-btn" data-action="submitExam" data-id="${row.id}">${row.submitted ? "重新提交" : "提交测试"}</button></div>`;
  }
  if (state.user && state.user.role === "STUDENT" && ["enrollments", "submissions", "examRecords"].includes(currentModule)) {
    return "";
  }
  const actions = cfg.actions || (cfg.fields ? ["edit", "delete"] : []);
  const names = { edit: "编辑", delete: "删除", approve: "通过", reject: "驳回", ban: "封禁", enable: "启用", close: "关闭", grade: "批改", reply: "回复", members: "成员", detail: "详情" };
  return `<div class="actions">${actions.map(a => `<button class="${a === "delete" ? "danger-btn" : "ghost-btn"}" data-action="${a}" data-id="${row.id}">${names[a]}</button>`).join("")}</div>`;
}

function renderCourseActions(row) {
  if (state.user && state.user.role === "TEACHER") {
    const actions = [`<button class="ghost-btn" data-action="detail" data-id="${row.id}">课程详情</button>`];
    if (row.status === "REJECTED") {
      actions.push(`<button class="ghost-btn" data-action="edit" data-id="${row.id}">编辑后重提</button>`);
    }
    if (row.status === "APPROVED") {
      actions.push(`<button class="ghost-btn" data-action="edit" data-id="${row.id}">编辑</button>`);
      actions.push(`<button class="danger-btn" data-action="delete" data-id="${row.id}">删除</button>`);
    }
    return `<div class="actions">${actions.join("")}</div>`;
  }
  if (state.user && state.user.role === "ADMIN") {
    const actions = [];
    if (row.status !== "APPROVED") actions.push(["approve", "通过", "ghost-btn"]);
    if (row.status !== "REJECTED") actions.push(["reject", "驳回", "ghost-btn"]);
    actions.push(["delete", "删除", "danger-btn"]);
    return `<div class="actions">${actions.map(a => `<button class="${a[2]}" data-action="${a[0]}" data-id="${row.id}">${a[1]}</button>`).join("")}</div>`;
  }
  return "";
}

function bindRowActions(rows) {
  document.querySelectorAll("[data-action]").forEach(btn => {
    btn.onclick = async () => {
      const row = rows.find(x => String(x.id) === String(btn.dataset.id));
      const action = btn.dataset.action;
      const cfg = moduleConfig[currentModule];
      if (action === "submitAssignment") {
        return quickStudentForm("提交作业", "/submissions", { assignmentId: row.id }, [["answer", "文字答案", "textarea"], ["fileUrl", "附件", "upload:submissions"]], renderAssignmentSubmitInfo(row), loadList);
      }
      if (action === "submitExam") {
        return quickStudentForm("提交测试", "/exam-records", { examId: row.id }, [["answer", "答案", "textarea"]], renderExamSubmitInfo(row), loadList);
      }
      if (action === "grade") return openGradeForm(row);
      if (action === "reply") return openDiscussionReplyForm(row);
      if (action === "members") return openGroupMembers(row);
      if (action === "detail" && currentModule === "courses") return openTeacherCourseDetail(row.id);
      if (action === "edit") return openForm(row);
      if (action === "delete") {
        if (!confirm("确认删除这条数据吗？")) return;
        await api(`${cfg.endpoint}/${row.id}`, { method: "DELETE" });
      }
      if (["approve", "reject", "ban", "enable", "close"].includes(action)) {
        await api(`${cfg.endpoint}/${row.id}/${action}`, { method: "POST", body: "{}" });
      }
      await loadOptions();
      loadList();
    };
  });
}

function openForm(row = {}) {
  const cfg = moduleConfig[currentModule];
  editingId = row.id || null;
  const mask = document.querySelector("#modal");
  mask.innerHTML = `<div class="modal-panel"><h3>${editingId ? "编辑" : "新增"}${cfg.title}</h3>
    <form id="dataForm" class="form-grid">${formFields(cfg).map(f => renderField(f, row)).join("")}</form>
    <div class="actions" style="justify-content:flex-end;margin-top:16px"><button class="ghost-btn" data-cancel>取消</button><button class="primary-btn" data-save>保存</button></div></div>`;
  mask.classList.add("show");
  bindFormSelects(row);
  bindUploads();
  document.querySelector("[data-cancel]").onclick = () => mask.classList.remove("show");
  document.querySelector("[data-save]").onclick = saveForm;
}

function formFields(cfg) {
  if (currentModule === "courses" && state.user && state.user.role === "TEACHER") {
    return cfg.fields.filter(field => !["teacherId", "status"].includes(field[0]));
  }
  return cfg.fields;
}

function openGradeForm(row) {
  const isSubmission = currentModule === "submissions";
  const mask = document.querySelector("#modal");
  const attachment = row.fileUrl ? `<p><strong>附件：</strong><a class="badge" href="${row.fileUrl}" target="_blank">${fileName(row.fileUrl)}</a></p>` : "";
  const commentField = isSubmission ? `<div class="form-item" style="grid-column:1/-1"><label>评语</label><textarea name="comment">${row.comment || ""}</textarea></div>` : "";
  mask.innerHTML = `<div class="modal-panel">
    <h3>${isSubmission ? "作业批改" : "成绩批改"}</h3>
    <div class="submit-info">
      <h4>${isSubmission ? row.assignmentTitle || "" : row.examTitle || ""}</h4>
      <p><strong>学习者：</strong>${row.studentName || ""}</p>
      <p><strong>提交内容：</strong>${row.answer || ""}</p>
      ${attachment}
      <p><strong>提交时间：</strong>${row.submittedAt || ""}</p>
      <p><strong>当前状态：</strong>${row.statusText || ""}</p>
    </div>
    <form id="gradeForm" class="form-grid">
      <div class="form-item"><label>得分</label><input name="score" value="${row.score || ""}"></div>
      ${commentField}
    </form>
    <div class="actions" style="justify-content:flex-end;margin-top:16px"><button class="ghost-btn" data-cancel>取消</button><button class="primary-btn" data-grade-save>保存批改</button></div>
  </div>`;
  mask.classList.add("show");
  document.querySelector("[data-cancel]").onclick = () => mask.classList.remove("show");
  document.querySelector("[data-grade-save]").onclick = async () => {
    const body = Object.assign({ id: row.id }, Object.fromEntries(new FormData(document.querySelector("#gradeForm")).entries()));
    const endpoint = isSubmission ? "/submissions/grade" : "/exam-records/grade";
    await api(endpoint, { method: "PUT", body: JSON.stringify(body) });
    mask.classList.remove("show");
    loadList();
  };
}

function openDiscussionReplyForm(row) {
  const detailHtml = `<div class="submit-info">
    <h4>${row.courseName || ""}</h4>
    <p><strong>提问人：</strong>${row.userName || ""}</p>
    <p><strong>原内容：</strong>${row.content || ""}</p>
  </div>`;
  quickStudentForm("回复讨论", "/discussions", { courseId: row.courseId, parentId: row.id }, [["content", "回复内容", "textarea"]], detailHtml, loadList);
}

async function openGroupMembers(row, afterChange) {
  const mask = document.querySelector("#modal");
  const availableResult = await api(`/groups/${row.id}/available-students`);
  const availableStudents = availableResult.data || [];
  mask.innerHTML = `<div class="modal-panel">
    <h3>管理分组成员</h3>
    <div class="submit-info">
      <h4>${row.name || ""}</h4>
      <p><strong>所属课程：</strong>${row.courseName || ""}</p>
      <p>${row.description || ""}</p>
    </div>
    <div class="member-add">
      <select name="memberStudentId">
        <option value="">选择学习者</option>
        ${availableStudents.map(item => `<option value="${item.studentId}">${item.studentName} · ${item.progress || 0}% · ${item.statusText || ""}</option>`).join("")}
      </select>
      <button class="primary-btn" data-member-add>添加成员</button>
    </div>
    <div class="member-list" data-member-list></div>
    <div class="actions" style="justify-content:flex-end;margin-top:16px"><button class="ghost-btn" data-cancel>关闭</button></div>
  </div>`;
  mask.classList.add("show");

  const loadMembers = async () => {
    const result = await api(`/groups/${row.id}/members`);
    const members = result.data || [];
    document.querySelector("[data-member-list]").innerHTML = members.length ? members.map(member => `
      <div class="member-row">
        <div class="member-user">${member.studentAvatar ? `<img class="avatar" src="${member.studentAvatar}" alt="">` : ""}<span>${member.studentName || ""}</span></div>
        <button class="danger-btn" data-member-delete="${member.id}">移除</button>
      </div>`).join("") : `<div class="empty-text">暂无成员</div>`;
    document.querySelectorAll("[data-member-delete]").forEach(btn => {
      btn.onclick = async () => {
        await api(`/groups/members/${btn.dataset.memberDelete}`, { method: "DELETE" });
        await loadMembers();
        if (afterChange) {
          mask.classList.remove("show");
          afterChange();
        } else {
          loadList();
        }
      };
    });
  };

  document.querySelector("[data-cancel]").onclick = () => mask.classList.remove("show");
  document.querySelector("[data-member-add]").onclick = async () => {
    const studentId = document.querySelector("[name=memberStudentId]").value;
    if (!studentId) return alert("请选择学习者");
    await api(`/groups/${row.id}/members/${studentId}`, { method: "POST", body: "{}" });
    document.querySelector("[name=memberStudentId]").value = "";
    await loadMembers();
    if (afterChange) {
      mask.classList.remove("show");
      afterChange();
    } else {
      loadList();
    }
  };
  await loadMembers();
}

async function openTeacherCourseDetail(courseId) {
  const [result, groupResult] = await Promise.all([
    api(`/courses/${courseId}/teacher-detail`),
    api(`/groups/page?pageNum=1&pageSize=100&courseId=${courseId}`)
  ]);
  const data = result.data || {};
  const course = data.course || {};
  const students = data.students || [];
  const groups = groupResult.data ? groupResult.data.records || [] : [];
  document.querySelector("#content").innerHTML = `<div class="topbar">
    <h2 class="page-title">课程详情</h2>
    <div data-user-name>${state.user ? state.user.realName + " · " + roleText(state.user.role) : ""}</div>
  </div>
  <div class="teacher-course-detail">
    <div class="detail-hero">
      <img src="${course.cover || ""}" alt="">
      <div class="detail-main">
        <button class="ghost-btn" data-back-list>返回课程管理</button>
        <h2>${course.title || ""}</h2>
        <p class="muted">${course.categoryName || ""} · ${levelText(course.level)} · ${course.durationMinutes || 0} 分钟 · ${course.statusText || ""}</p>
        <p>${course.description || ""}</p>
      </div>
    </div>
    <div class="stats-grid compact">
      <div class="stat-card"><span>学习者</span><strong>${data.studentCount || 0}</strong></div>
      <div class="stat-card"><span>作业数</span><strong>${data.assignmentCount || 0}</strong></div>
      <div class="stat-card"><span>测试数</span><strong>${data.examCount || 0}</strong></div>
      <div class="stat-card"><span>总分</span><strong>${data.totalPossible || 0}</strong></div>
    </div>
    <section class="detail-section full">
      <div class="section-head"><h3>分组标签管理</h3><button class="primary-btn" data-course-group-add="${course.id}">新增分组标签</button></div>
      <div class="table-wrap"><table>
        <thead><tr><th>分组标签</th><th>说明</th><th>成员数</th><th>创建时间</th><th>操作</th></tr></thead>
        <tbody>${groups.length ? groups.map(group => `<tr>
          <td>${group.name || ""}</td>
          <td>${group.description || ""}</td>
          <td>${group.memberCount || 0}</td>
          <td>${group.createdAt || ""}</td>
          <td><div class="actions"><button class="ghost-btn" data-course-group-members="${group.id}">成员</button><button class="ghost-btn" data-course-group-edit="${group.id}">编辑</button><button class="danger-btn" data-course-group-delete="${group.id}">删除</button></div></td>
        </tr>`).join("") : `<tr><td colspan="5" class="empty-text">暂无分组标签</td></tr>`}</tbody>
      </table></div>
    </section>
    <section class="detail-section full">
      <h3>学习成绩与排名</h3>
      <div class="table-wrap"><table>
        <thead><tr><th>排名</th><th>学习者</th><th>分组标签</th><th>进度</th><th>学习分钟</th><th>作业</th><th>测试</th><th>总分</th><th>最近学习</th></tr></thead>
        <tbody>${students.length ? students.map(item => `<tr>
          <td>${item.rank || ""}</td>
          <td><div class="member-user">${item.studentAvatar ? `<img class="avatar" src="${item.studentAvatar}" alt="">` : ""}<span>${item.studentName || ""}</span></div></td>
          <td>${item.groupNames || "<span class='muted'>未分组</span>"}</td>
          <td>${item.progress || 0}% · ${item.studyStatusText || ""}</td>
          <td>${item.learnMinutes || 0}</td>
          <td>${item.assignmentScore || 0} <span class="muted">(${item.submittedAssignments || "0/0"})</span></td>
          <td>${item.examScore || 0} <span class="muted">(${item.submittedExams || "0/0"})</span></td>
          <td><strong>${item.totalScore || 0}</strong> / ${item.totalPossible || 0}</td>
          <td>${item.lastStudyTime || ""}</td>
        </tr>`).join("") : `<tr><td colspan="9" class="empty-text">暂无学习者数据</td></tr>`}</tbody>
      </table></div>
    </section>
  </div>`;
  document.querySelector("[data-back-list]").onclick = () => openModule("courses");
  bindTeacherCourseGroupActions(course.id, groups);
}

function bindTeacherCourseGroupActions(courseId, groups) {
  const add = document.querySelector("[data-course-group-add]");
  if (add) add.onclick = () => openCourseGroupForm(courseId);
  document.querySelectorAll("[data-course-group-members]").forEach(btn => {
    btn.onclick = () => {
      const group = groups.find(item => String(item.id) === String(btn.dataset.courseGroupMembers));
      if (group) openGroupMembers(group, () => openTeacherCourseDetail(courseId));
    };
  });
  document.querySelectorAll("[data-course-group-edit]").forEach(btn => {
    btn.onclick = () => {
      const group = groups.find(item => String(item.id) === String(btn.dataset.courseGroupEdit));
      if (group) openCourseGroupForm(courseId, group);
    };
  });
  document.querySelectorAll("[data-course-group-delete]").forEach(btn => {
    btn.onclick = async () => {
      if (!confirm("确认删除这个分组标签吗？")) return;
      await api(`/groups/${btn.dataset.courseGroupDelete}`, { method: "DELETE" });
      openTeacherCourseDetail(courseId);
    };
  });
}

function openCourseGroupForm(courseId, row = {}) {
  const mask = document.querySelector("#modal");
  const editing = !!row.id;
  mask.innerHTML = `<div class="modal-panel"><h3>${editing ? "编辑" : "新增"}分组标签</h3>
    <form id="courseGroupForm" class="form-grid">
      <div class="form-item"><label>分组标签</label><input name="name" value="${row.name || ""}"></div>
      <div class="form-item" style="grid-column:1/-1"><label>说明</label><textarea name="description">${row.description || ""}</textarea></div>
    </form>
    <div class="actions" style="justify-content:flex-end;margin-top:16px"><button class="ghost-btn" data-cancel>取消</button><button class="primary-btn" data-save-group>保存</button></div>
  </div>`;
  mask.classList.add("show");
  document.querySelector("[data-cancel]").onclick = () => mask.classList.remove("show");
  document.querySelector("[data-save-group]").onclick = async () => {
    const body = Object.assign({ id: row.id, courseId }, Object.fromEntries(new FormData(document.querySelector("#courseGroupForm")).entries()));
    await api("/groups", { method: editing ? "PUT" : "POST", body: JSON.stringify(body) });
    mask.classList.remove("show");
    openTeacherCourseDetail(courseId);
  };
}

function renderField(field, row) {
  const [name, label, type = "input"] = field;
  const value = row[name] || "";
  if (type === "textarea") return `<div class="form-item" style="grid-column:1/-1"><label>${label}</label><textarea name="${name}">${value}</textarea></div>`;
  if (type.startsWith("select:")) return `<div class="form-item"><label>${label}</label><select name="${name}" data-form-dict="${type.replace("select:", "")}"></select></div>`;
  if (type.startsWith("upload:")) {
    const module = type.replace("upload:", "");
    return `<div class="form-item" style="grid-column:1/-1"><label>${label}</label><input type="hidden" name="${name}" value="${value}"><div class="upload-file-name" data-file-name="${name}">${value ? fileName(value) : "未上传文件"}</div><div class="upload-row"><input type="file" data-upload="${module}" data-target="${name}"></div><div class="upload-preview" data-preview="${name}">${value && /\.(png|jpg|jpeg|webp)$/i.test(value) ? `<img src="${value}" alt="">` : ""}</div></div>`;
  }
  return `<div class="form-item"><label>${label}</label><input name="${name}" value="${value}"></div>`;
}

function bindFormSelects(row) {
  document.querySelectorAll("[data-form-dict]").forEach(select => {
    const key = select.dataset.formDict;
    let rows = dictionaries[key] || [];
    if (key === "categories") rows = state.options.categories.map(x => [x.id, x.name]);
    if (key === "courses") rows = state.options.courses.map(x => [x.id, x.title]);
    if (key === "teachers") rows = state.options.teachers.map(x => [x.id, x.realName]);
    if (key === "students") rows = state.options.students.map(x => [x.id, x.realName]);
    select.innerHTML = `<option value="">请选择</option>` + rows.map(x => `<option value="${x[0]}">${x[1]}</option>`).join("");
    select.value = row[select.name] || "";
  });
  fillAssignmentAndExamSelects(row);
}

async function fillAssignmentAndExamSelects(row) {
  const assignments = document.querySelector("[name=assignmentId]");
  if (assignments) {
    const res = await api("/assignments/page?pageNum=1&pageSize=100");
    assignments.innerHTML = `<option value="">请选择</option>` + (res.data.records || []).map(x => `<option value="${x.id}">${x.title}</option>`).join("");
    assignments.value = row.assignmentId || "";
  }
  const exams = document.querySelector("[name=examId]");
  if (exams) {
    const res = await api("/exams/page?pageNum=1&pageSize=100");
    exams.innerHTML = `<option value="">请选择</option>` + (res.data.records || []).map(x => `<option value="${x.id}">${x.title}</option>`).join("");
    exams.value = row.examId || "";
  }
}

function bindUploads() {
  document.querySelectorAll("[data-upload]").forEach(input => {
    input.onchange = async () => {
      const target = input.dataset.target;
      if (!input.files.length) return;
      const fileNameBox = document.querySelector(`[data-file-name=${target}]`);
      if (fileNameBox) fileNameBox.textContent = "上传中：" + input.files[0].name;
      const form = new FormData();
      form.append("file", input.files[0]);
      form.append("module", input.dataset.upload);
      const result = await api("/files/upload", { method: "POST", body: form });
      if (result.code !== 200) return alert(result.message || "上传失败");
      document.querySelector(`[name=${target}]`).value = result.data.url;
      if (fileNameBox) fileNameBox.textContent = result.data.name || fileName(result.data.url);
      const preview = document.querySelector(`[data-preview=${target}]`);
      preview.innerHTML = /\.(png|jpg|jpeg|webp)$/i.test(result.data.url) ? `<img src="${result.data.url}" alt="">` : `<span class="badge">已上传</span>`;
    };
  });
}

async function saveForm() {
  const cfg = moduleConfig[currentModule];
  const body = Object.fromEntries(new FormData(document.querySelector("#dataForm")).entries());
  if (editingId) body.id = editingId;
  const method = editingId ? "PUT" : "POST";
  await api(cfg.endpoint, { method, body: JSON.stringify(body) });
  document.querySelector("#modal").classList.remove("show");
  await loadOptions();
  loadList();
}

async function renderDashboard() {
  const root = document.querySelector("#content");
  root.innerHTML = `<div class="topbar"><h2 class="page-title">数据统计</h2><div data-user-name></div></div>
    <div class="stats-grid" id="stats"></div>
    <div class="charts"><div class="chart-panel" id="chartCategory"></div><div class="chart-panel" id="chartTrend"></div></div>`;
  if (state.user) document.querySelector("[data-user-name]").textContent = state.user.realName + " · " + roleText(state.user.role);
  const result = await api("/dashboard/stats");
  const data = result.data || {};
  document.querySelector("#stats").innerHTML = [
    ["用户总数", data.userCount], ["教师数量", data.teacherCount], ["学习者数量", data.studentCount], ["课程数量", data.courseCount],
    ["选课记录", data.enrollmentCount], ["作业数量", data.assignmentCount], ["作业提交", data.submittedCount], ["测试记录", data.examRecordCount]
  ].map(x => `<div class="stat-card"><span>${x[0]}</span><strong>${x[1] || 0}</strong></div>`).join("");
  echarts.init(document.querySelector("#chartCategory")).setOption({
    title: { text: "课程分类统计" },
    tooltip: {},
    series: [{ type: "pie", radius: "60%", data: data.courseByCategory || [] }]
  });
  echarts.init(document.querySelector("#chartTrend")).setOption({
    title: { text: "用户活跃趋势" },
    tooltip: {},
    xAxis: { type: "category", data: (data.activityTrend || []).map(x => x.name) },
    yAxis: { type: "value" },
    series: [{ type: "line", smooth: true, data: (data.activityTrend || []).map(x => x.value) }]
  });
}

async function initPortal(type) {
  guard();
  await loadOptions();
  document.querySelector("[data-user-name]").textContent = state.user.realName + " · " + roleText(state.user.role);
  document.querySelector("[data-logout]").onclick = () => { localStorage.clear(); location.href = "/login"; };
  if (type === "student") loadStudentCourses();
  if (type === "teacher") initApp("courses");
}

async function loadStudentCourses() {
  const result = await api("/courses/page?pageNum=1&pageSize=100&status=APPROVED");
  const rows = result.data.records || [];
  document.querySelector("#content").innerHTML = `<div class="student-filter">
    <div class="filter-field"><label>课程名称</label><input name="keyword" placeholder="输入课程名称"></div>
    <div class="filter-field"><label>课程分类</label><select name="categoryId"><option value="">全部分类</option>${state.options.categories.map(x => `<option value="${x.id}">${x.name}</option>`).join("")}</select></div>
    <div class="filter-field"><label>难度等级</label><select name="level"><option value="">全部难度</option><option value="BEGINNER">入门</option><option value="INTERMEDIATE">进阶</option><option value="ADVANCED">高级</option></select></div>
    <div class="filter-actions"><button class="primary-btn" data-filter-course>查询</button></div>
  </div><div class="course-grid" id="courseGrid"></div>`;
  const render = list => {
    document.querySelector("#courseGrid").innerHTML = list.map(item => `<div class="course-card"><img src="${item.cover}" alt=""><div class="body"><h3>${item.title}</h3><p class="muted">${item.categoryName} · ${levelText(item.level)} · ${item.teacherName}</p><p>${item.description || ""}</p><div class="actions"><button class="primary-btn" data-detail="${item.id}">课程详情</button></div></div></div>`).join("");
    bindStudentButtons();
  };
  render(rows);
  document.querySelector("[data-filter-course]").onclick = () => {
    const keyword = document.querySelector("[name=keyword]").value;
    const categoryId = document.querySelector("[name=categoryId]").value;
    const level = document.querySelector("[name=level]").value;
    render(rows.filter(x => (!keyword || x.title.includes(keyword)) && (!categoryId || String(x.categoryId) === categoryId) && (!level || x.level === level)));
  };
}

function bindStudentButtons() {
  document.querySelectorAll("[data-detail]").forEach(btn => btn.onclick = () => loadCourseDetail(btn.dataset.detail));
  document.querySelectorAll("[data-join]").forEach(btn => btn.onclick = async () => {
    await api(`/enrollments/join/${btn.dataset.join}`, { method: "POST", body: "{}" });
    alert("已加入课程");
  });
  document.querySelectorAll("[data-note]").forEach(btn => btn.onclick = () => quickStudentForm("学习笔记", "/notes", { courseId: btn.dataset.note }, [["content", "笔记内容", "textarea"]]));
  document.querySelectorAll("[data-review]").forEach(btn => btn.onclick = () => quickStudentForm("课程评价", "/reviews", { courseId: btn.dataset.review }, [["rating", "评分"], ["content", "评价内容", "textarea"]]));
  document.querySelectorAll("[data-discuss]").forEach(btn => btn.onclick = () => openDiscussionForm(btn.dataset.discuss));
}

async function loadCourseDetail(courseId) {
  const [courseRes, materialRes, discussionRes, reviewRes] = await Promise.all([
    api(`/courses/${courseId}`),
    api(`/materials?courseId=${courseId}`),
    api(`/discussions?courseId=${courseId}`),
    api(`/reviews?courseId=${courseId}`)
  ]);
  const course = courseRes.data || {};
  const materials = materialRes.data || [];
  const discussions = discussionRes.data || [];
  const reviews = reviewRes.data || [];
  document.querySelector("#content").innerHTML = `<div class="course-detail">
    <div class="detail-hero">
      <img src="${course.cover || ""}" alt="">
      <div class="detail-main">
        <button class="ghost-btn" data-back-course>返回课程中心</button>
        <h2>${course.title || ""}</h2>
        <p class="muted">${course.categoryName || ""} · ${levelText(course.level)} · ${course.durationMinutes || 0} 分钟 · ${course.enrollCount || 0} 人学习</p>
        <div class="teacher-line">${course.teacherAvatar ? `<img class="avatar" src="${course.teacherAvatar}" alt="">` : ""}<span>${course.teacherName || ""}</span></div>
        <p>${course.description || ""}</p>
        <div class="actions"><button class="primary-btn" data-join="${course.id}">加入学习</button><button class="ghost-btn" data-note="${course.id}">记笔记</button><button class="ghost-btn" data-review="${course.id}">评价课程</button></div>
      </div>
    </div>
    <div class="detail-grid">
      <section class="detail-section"><h3>学习目标</h3><p>${course.objective || ""}</p></section>
      <section class="detail-section"><h3>课程大纲</h3><p>${course.outline || ""}</p></section>
      <section class="detail-section"><h3>课程资料</h3>${renderMaterials(materials, course)}</section>
      <section class="detail-section"><div class="section-head"><h3>课程讨论</h3><button class="primary-btn" data-discuss="${course.id}">我要提问</button></div>${renderDiscussions(discussions)}</section>
      <section class="detail-section full"><h3>课程评价</h3>${renderReviews(reviews)}</section>
    </div>
  </div>`;
  document.querySelector("[data-back-course]").onclick = loadStudentCourses;
  bindStudentButtons();
  bindStudyProgress(course.id);
}

function renderMaterials(materials, course) {
  const rows = materials.length ? materials : [
    course.videoUrl ? { name: "课程主视频", typeText: "视频", fileUrl: course.videoUrl } : null,
    course.docUrl ? { name: "课程主文档", typeText: "文档", fileUrl: course.docUrl } : null
  ].filter(Boolean);
  if (!rows.length) return `<p class="muted">暂无课程资料</p>`;
  return `<div class="material-list">${rows.map(item => {
    const type = item.typeText || statusText(item.type);
    if (type === "视频" || isDirectVideoUrl(item.fileUrl)) {
      return renderVideoMaterial(item);
    }
    return `<a href="${item.fileUrl}" target="_blank" class="material-item"><span>${item.name}</span><em>${type}</em></a>`;
  }).join("")}</div>`;
}

function renderVideoMaterial(item) {
  const url = item.fileUrl || "";
  const bvid = bilibiliBvid(url);
  if (isDirectVideoUrl(url)) {
    return `<div class="video-material"><div class="material-title">${item.name}</div><video controls preload="metadata" src="${url}" data-study-video></video></div>`;
  }
  if (bvid) {
    const embedUrl = `https://player.bilibili.com/player.html?bvid=${bvid}&page=1&high_quality=1`;
    return `<div class="video-material"><div class="material-title">${item.name}</div><iframe src="${embedUrl}" allowfullscreen allow="fullscreen; picture-in-picture"></iframe><a href="${url}" target="_blank" rel="noopener" class="video-link">打开原视频</a></div>`;
  }
  return `<a href="${url}" target="_blank" rel="noopener" class="material-item"><span>${item.name}</span><em>视频</em></a>`;
}

function isDirectVideoUrl(url) {
  return /\.(mp4|webm|ogg)(\?.*)?$/i.test(url || "");
}

function bilibiliBvid(url) {
  const match = String(url || "").match(/(?:bilibili\.com\/video\/)?(BV[0-9A-Za-z]+)/i);
  return match ? match[1] : "";
}

function renderDiscussions(rows) {
  if (!rows.length) return `<p class="muted">暂无课程讨论</p>`;
  const groups = [];
  const groupMap = {};
  rows.forEach(item => {
    if (!item.parentId) {
      const group = groupMap[item.id] || { question: item, replies: [] };
      group.question = item;
      groupMap[item.id] = group;
      if (!groups.includes(group)) groups.push(group);
    }
  });
  rows.forEach(item => {
    if (!item.parentId) return;
    const parentKey = item.parentId;
    let group = groupMap[parentKey];
    if (!group) {
      group = { question: { id: parentKey, content: item.parentContent || "原问题", statusText: "已回复" }, replies: [] };
      groupMap[parentKey] = group;
      groups.push(group);
    }
    group.replies.push(item);
  });
  return `<div class="discussion-list">${groups.map(group => {
    const question = group.question || {};
    const replies = group.replies.sort((a, b) => String(a.createdAt || "").localeCompare(String(b.createdAt || "")));
    return `<div class="discussion-thread">
      <div class="discussion-main">
        ${renderDiscussionPerson(question, "提问")}
        <p>${question.content || ""}</p>
        <div class="discussion-meta"><span>${question.createdAt || ""}</span><span>${question.statusText || ""}</span></div>
      </div>
      ${replies.length ? `<div class="discussion-children">${replies.map(reply => `<div class="discussion-child">${renderDiscussionPerson(reply, "回答")}<p>${reply.content || ""}</p><div class="discussion-meta"><span>${reply.createdAt || ""}</span></div></div>`).join("")}</div>` : `<div class="discussion-empty">暂无回答</div>`}
    </div>`;
  }).join("")}</div>`;
}

function renderDiscussionPerson(item, label) {
  return `<div class="discussion-person">${item.userAvatar ? `<img class="avatar" src="${item.userAvatar}" alt="">` : ""}<strong>${item.userName || ""}</strong><span>${label}</span></div>`;
}

function renderReviews(rows) {
  if (!rows.length) return `<p class="muted">暂无课程评价</p>`;
  return rows.map(item => `<div class="comment-row">${item.studentAvatar ? `<img class="avatar" src="${item.studentAvatar}" alt="">` : ""}<div><strong>${item.studentName || ""} · ${item.rating || 0} 分</strong><p>${item.content || ""}</p><span class="muted">${item.createdAt || ""}</span></div></div>`).join("");
}

function openDiscussionForm(courseId) {
  quickStudentForm("我要提问", "/discussions", { courseId }, [["content", "问题内容", "textarea"]], "", () => loadCourseDetail(courseId));
}

function bindStudyProgress(courseId) {
  document.querySelectorAll("[data-study-video]").forEach(video => {
    let lastReportSecond = 0;
    const report = () => {
      if (!video.duration || Number.isNaN(video.duration)) return;
      const currentSecond = Math.floor(video.currentTime || 0);
      if (!video.ended && currentSecond - lastReportSecond < 15) return;
      lastReportSecond = currentSecond;
      const progress = Math.min(100, Math.round((video.currentTime / video.duration) * 10000) / 100);
      const learnMinutes = Math.max(1, Math.ceil(video.currentTime / 60));
      api("/enrollments/progress", {
        method: "PUT",
        body: JSON.stringify({ courseId, progress, learnMinutes })
      });
    };
    video.addEventListener("timeupdate", report);
    video.addEventListener("ended", report);
  });
}

function renderAssignmentSubmitInfo(row) {
  return `<div class="submit-info"><h4>${row.title || ""}</h4><p><strong>所属课程：</strong>${row.courseName || ""}</p><p><strong>作业说明：</strong>${row.description || ""}</p><p><strong>满分：</strong>${row.totalScore || ""}　<strong>截止时间：</strong>${row.dueTime || ""}</p></div>`;
}

function renderExamSubmitInfo(row) {
  return `<div class="submit-info"><h4>${row.title || ""}</h4><p><strong>所属课程：</strong>${row.courseName || ""}</p><p><strong>题型：</strong>${row.typeText || ""}　<strong>满分：</strong>${row.totalScore || ""}</p><p><strong>题目：</strong>${row.question || ""}</p></div>`;
}

function fileName(path) {
  if (!path) return "";
  return String(path).split(/[\\/]/).pop();
}

function quickStudentForm(title, endpoint, base, fields, detailHtml = "", afterSave) {
  const mask = document.querySelector("#modal");
  mask.innerHTML = `<div class="modal-panel"><h3>${title}</h3>${detailHtml}<form id="dataForm" class="form-grid">${fields.map(f => renderField(f, {})).join("")}</form><div class="actions" style="justify-content:flex-end;margin-top:16px"><button class="ghost-btn" data-cancel>取消</button><button class="primary-btn" data-save>保存</button></div></div>`;
  mask.classList.add("show");
  bindUploads();
  document.querySelector("[data-cancel]").onclick = () => mask.classList.remove("show");
  document.querySelector("[data-save]").onclick = async () => {
    const body = Object.assign({}, base, Object.fromEntries(new FormData(document.querySelector("#dataForm")).entries()));
    await api(endpoint, { method: "POST", body: JSON.stringify(body) });
    mask.classList.remove("show");
    alert("保存成功");
    if (afterSave) afterSave();
  };
}
