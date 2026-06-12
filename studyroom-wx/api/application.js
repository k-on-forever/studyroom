const { request } = require("../utils/http");

/** 我的预约列表 */
function getLicationList() {
  return request("/applet/myAppointment", { method: "POST", data: {} });
}

/** 预约详情（本人） */
function getAppointmentDetail(id) {
  return request("/applet/appointment/" + encodeURIComponent(id), {
    method: "GET",
    data: {},
  });
}

/** 预约前询价 */
function quoteAppointment(body) {
  return request("/applet/appointment/quote", { method: "POST", data: body });
}

/** 取消预约（仅待签到可取消；使用中请签退） */
function cancelLication(id) {
  return request("/applet/cancel?id=" + encodeURIComponent(id), { method: "POST", data: {} });
}

/** 结束学习（仅已签到使用中） */
function overLication(id) {
  return request("/applet/over?id=" + encodeURIComponent(id), { method: "POST", data: {} });
}

/** 签到（待签到 -> 使用中） */
function signInLication(id) {
  return request("/applet/signIn?id=" + encodeURIComponent(id), { method: "POST", data: {} });
}

module.exports = {
  getLicationList,
  getAppointmentDetail,
  quoteAppointment,
  cancelLication,
  overLication,
  signInLication,
};
