const { request } = require("../utils/http");

function sendResetPasswordSms(account) {
  return request("/applet/password/resetSmsSend", {
    method: "POST",
    data: { account },
  });
}

function resetPassword(form) {
  return request("/applet/password/reset", { method: "POST", data: form });
}

module.exports = { sendResetPasswordSms, resetPassword };
