const { request } = require("../utils/http");

function register(form) {
  return request("/applet/register", { method: "POST", data: form });
}

/** 注册用验证码（模拟，验证码见服务端日志） */
function sendRegisterSms(account) {
  return request("/applet/register/smsSend", {
    method: "POST",
    data: { account },
  });
}

module.exports = { register, sendRegisterSms };
