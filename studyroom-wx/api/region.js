const { request } = require("../utils/http");

function getFloorList() {
  return request("/applet/getAllFloor", { method: "GET" });
}

function getRoomList(floorId) {
  return request("/applet/getRoomByFloor?id=" + encodeURIComponent(floorId || ""), {
    method: "GET",
  });
}

function getSeatList(roomId) {
  return request("/applet/getSeatByRoom?id=" + encodeURIComponent(roomId || ""), {
    method: "GET",
  });
}

/** 提交预约：body 需含 seatDay，格式 yyyy-MM-dd/HH:mm-HH:mm */
function addSeat(dataFrom) {
  return request("/applet/appointment", { method: "POST", data: dataFrom });
}

module.exports = {
  getFloorList,
  getRoomList,
  getSeatList,
  addSeat,
};
