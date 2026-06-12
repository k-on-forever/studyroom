const { request } = require("../utils/http");

function addMessage(body) {
  return request("/applet/userMessage", { method: "POST", data: body });
}

module.exports = { addMessage };
