const { request } = require("../utils/http");

/** 后台「仅手机号可订」白名单：展示当前用户可享的专属时段 */
function mineWhitelist() {
  return request("/applet/seatAccessRule/mineWhitelist", { method: "GET" });
}

module.exports = { mineWhitelist };
