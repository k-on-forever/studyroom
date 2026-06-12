const { request } = require("../utils/http");

function browseHold(data) {
  return request("/applet/seatBrowseLock/hold", { method: "POST", data });
}

function browseRelease(data) {
  return request("/applet/seatBrowseLock/release", { method: "POST", data });
}

function browseMine() {
  return request("/applet/seatBrowseLock/mine", { method: "GET" });
}

function browseOverlay(roomId, bizDate, seatDayOptional) {
  let q =
    "?roomId=" +
    encodeURIComponent(String(roomId)) +
    "&bizDate=" +
    encodeURIComponent(bizDate);
  if (seatDayOptional && String(seatDayOptional).trim()) {
    q +=
      "&seatDay=" + encodeURIComponent(String(seatDayOptional).trim());
  }
  return request("/applet/seatBrowseLock/overlay" + q, { method: "GET" });
}

module.exports = { browseHold, browseRelease, browseMine, browseOverlay };
