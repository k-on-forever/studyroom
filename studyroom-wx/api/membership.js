const { request } = require("../utils/http");

function listCards() {
  return request("/applet/membership/cards", { method: "GET" });
}

function createMembershipOrder(cardId) {
  return request("/applet/membership/order", {
    method: "POST",
    data: { cardId },
  });
}

function mockPayMembership(orderId) {
  return request("/applet/membership/mockPay", {
    method: "POST",
    data: { orderId },
  });
}

function mineMembership() {
  return request("/applet/membership/mine", { method: "GET" });
}

function activateMembership(orderId, activateDate) {
  return request("/applet/membership/activate", {
    method: "POST",
    data: { orderId, activateDate },
  });
}

module.exports = {
  listCards,
  createMembershipOrder,
  mockPayMembership,
  mineMembership,
  activateMembership,
};
