const { request } = require("../utils/http");

function getPayConfig() {
  return request("/applet/wxpay/config", { method: "GET" });
}

function prepayAppointment(appointmentBody) {
  return request("/applet/wxpay/prepay/appointment", {
    method: "POST",
    data: appointmentBody,
  });
}

function prepayMembership(orderId) {
  return request("/applet/wxpay/prepay/membership", {
    method: "POST",
    data: { orderId },
  });
}

function mockConfirmPay(outTradeNo) {
  return request("/applet/wxpay/mock/confirm", {
    method: "POST",
    data: { outTradeNo },
  });
}

function afterPay(outTradeNo) {
  return request("/applet/wxpay/after-pay", {
    method: "POST",
    data: { outTradeNo },
  });
}

module.exports = {
  getPayConfig,
  prepayAppointment,
  prepayMembership,
  mockConfirmPay,
  afterPay,
};
