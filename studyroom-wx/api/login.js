const { request } = require("../utils/http");

function login(data) {
  return request("/applet/login", { method: "POST", data });
}

module.exports = { login };
