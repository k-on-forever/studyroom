const { request } = require("../utils/http");

function getStudyStats() {
  return request("/applet/studyStats", { method: "GET", data: {} });
}

module.exports = { getStudyStats };
