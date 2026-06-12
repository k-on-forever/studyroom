const { request } = require("../utils/http");

/** 与后端 sys_reservation_rule.advance_booking_days 一致 */
function getReservationRule() {
  return request("/applet/reservation-rule", { method: "GET" });
}

module.exports = {
  getReservationRule,
};
