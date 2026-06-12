const { request } = require("../utils/http");

function getBookingConfig() {
  return request("/applet/config/booking", { method: "GET" });
}

module.exports = { getBookingConfig };
