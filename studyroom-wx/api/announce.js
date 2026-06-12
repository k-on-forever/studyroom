const { request } = require("../utils/http");

/** 公告分页：后端 data 为 { list, totalCount } */
function getAnnounceList(page = 1, limit = 10, keyword = "") {
  return request(
    "/applet/listNotice?page=" +
      encodeURIComponent(page) +
      "&limit=" +
      encodeURIComponent(limit) +
      (keyword ? "&keyword=" + encodeURIComponent(keyword) : ""),
    { method: "GET" }
  );
}

function getForbanList() {
  return request("/applet/listForBan", { method: "GET" });
}

function getMessageList() {
  return request("/applet/MessageList", { method: "GET" });
}

module.exports = {
  getAnnounceList,
  getForbanList,
  getMessageList,
};
