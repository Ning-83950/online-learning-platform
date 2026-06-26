const API = "/api";

function message(text) {
  const box = document.querySelector("[data-message]");
  if (box) box.textContent = text || "";
}

function quickLogin(account, password) {
  document.querySelector("[name=account]").value = account;
  document.querySelector("[name=password]").value = password;
  login();
}

async function postJson(url, body) {
  const res = await fetch(API + url, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(body)
  });
  return res.json();
}

async function login() {
  const account = document.querySelector("[name=account]").value.trim();
  const password = document.querySelector("[name=password]").value.trim();
  if (!account || !password) {
    message("请输入账号和密码");
    return;
  }
  const result = await postJson("/auth/login", { account, password });
  if (result.code !== 200) {
    message(result.message || "登录失败");
    return;
  }
  localStorage.setItem("satoken", result.data.token);
  localStorage.setItem("user", JSON.stringify(result.data.user));
  const role = result.data.user.role;
  if (role === "ADMIN") location.href = "/admin";
  if (role === "TEACHER") location.href = "/teacher";
  if (role === "STUDENT") location.href = "/student";
}

async function register() {
  const body = Object.fromEntries(new FormData(document.querySelector("form")).entries());
  if (!body.username || !body.password || !body.realName) {
    message("请填写用户名、密码和姓名");
    return;
  }
  if (body.password !== body.confirmPassword) {
    message("两次输入的密码不一致");
    return;
  }
  if (body.phone && !/^1[3-9]\d{9}$/.test(body.phone)) {
    message("请输入正确的手机号");
    return;
  }
  if (body.email && !/^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$/.test(body.email)) {
    message("请输入正确的邮箱地址");
    return;
  }
  delete body.confirmPassword;
  const result = await postJson("/auth/register", body);
  if (result.code !== 200) {
    message(result.message || "注册失败");
    return;
  }
  message("注册成功，请登录");
  setTimeout(() => location.href = "/login", 700);
}

async function forgotPassword() {
  const body = Object.fromEntries(new FormData(document.querySelector("form")).entries());
  if (!body.account || !body.realName || !body.newPassword || !body.confirmPassword) {
    message("请输入账号、姓名、新密码和重复密码");
    return;
  }
  if (body.newPassword !== body.confirmPassword) {
    message("两次输入的密码不一致");
    return;
  }
  delete body.confirmPassword;
  const result = await postJson("/auth/forgot-password", body);
  if (result.code !== 200) {
    message(result.message || "找回密码失败");
    return;
  }
  message("密码已重置，请返回登录");
}
