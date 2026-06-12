/**
 * 预约列表/详情共用：时段展示、状态文案、签到时间解析
 */

function formatSeatDayForDisplay(raw) {
  if (!raw || !String(raw).trim()) return "";
  return String(raw)
    .split("；")
    .map((p) => {
      const t = p.trim();
      const slash = t.indexOf("/");
      if (slash < 0) return t;
      const date = t.slice(0, slash).trim();
      const tail = t.slice(slash + 1).trim();
      return date + " " + tail.replace(/-/g, " 至 ");
    })
    .join("\n");
}

/** seatState: 0待签到 1使用中 2已取消 3已完成 4未签到取消 */
function stateText(seatState) {
  if (seatState === 0) return "待签到";
  if (seatState === 1) return "使用中";
  if (seatState === 2) return "已取消";
  if (seatState === 3) return "已完成";
  if (seatState === 4) return "未签到取消";
  return "未知";
}

function payLabel(item) {
  if (!item) return "";
  if (item.payStatus === 0) return "会员免费";
  if (item.payStatus === 1) {
    return "¥" + (item.payAmount != null ? item.payAmount : "0");
  }
  return "";
}

function parseSlotStartMs(it) {
  const raw = it && (it.seatDay || it);
  if (!raw || !String(raw).trim()) return null;
  const t = String(raw).trim();
  const slash = t.indexOf("/");
  if (slash < 0) return null;
  const ymd = t.slice(0, slash).trim();
  const startHm = t.slice(slash + 1).trim().split("-")[0].trim();
  const dp = ymd.split("-");
  const hp = startHm.split(":");
  if (dp.length < 3 || hp.length < 2) return null;
  return new Date(
    parseInt(dp[0], 10),
    parseInt(dp[1], 10) - 1,
    parseInt(dp[2], 10),
    parseInt(hp[0], 10),
    parseInt(hp[1], 10),
    0,
    0
  ).getTime();
}

function formatHmFromMs(ms) {
  const d = new Date(ms);
  const p = (n) => (n < 10 ? "0" + n : "" + n);
  return p(d.getHours()) + ":" + p(d.getMinutes());
}

function formatTs(ms) {
  if (ms == null || ms === "") return "";
  const n = typeof ms === "number" ? ms : parseInt(ms, 10);
  if (!n || Number.isNaN(n)) return "";
  const d = new Date(n);
  const p = (x) => (x < 10 ? "0" + x : "" + x);
  return (
    d.getFullYear() +
    "-" +
    p(d.getMonth() + 1) +
    "-" +
    p(d.getDate()) +
    " " +
    p(d.getHours()) +
    ":" +
    p(d.getMinutes())
  );
}

function bizDateOf(it) {
  if (it.bizDate && String(it.bizDate).length >= 10) {
    return String(it.bizDate).slice(0, 10);
  }
  if (it.seatDay && String(it.seatDay).length >= 10) {
    return String(it.seatDay).slice(0, 10);
  }
  return "";
}

function enrichForList(it) {
  const startMs = parseSlotStartMs(it);
  const now = Date.now();
  const beforeStart =
    it.seatState === 0 && startMs != null && !Number.isNaN(startMs) && now < startMs;
  const canSignIn = it.seatState === 0 && !beforeStart;
  return Object.assign({}, it, {
    bizDate: bizDateOf(it) || it.bizDate,
    seatDayDisplay: formatSeatDayForDisplay(it.seatDay),
    stateText: beforeStart ? "待开始" : stateText(it.seatState),
    payLabel: payLabel(it),
    canCancel: it.seatState === 0,
    canSignIn,
    signInHint: beforeStart && startMs ? formatHmFromMs(startMs) + " 后可签到" : "",
    canOver: it.seatState === 1,
  });
}

function enrichForDetail(raw) {
  const st = raw.seatState;
  const startMs = parseSlotStartMs(raw);
  const beforeStart =
    st === 0 && startMs != null && !Number.isNaN(startMs) && Date.now() < startMs;
  const canSignIn = st === 0 && !beforeStart;
  return Object.assign({}, raw, {
    seatDayDisplay: formatSeatDayForDisplay(raw.seatDay),
    checkInAtText: formatTs(raw.checkInAt),
    studyEndAtText: formatTs(raw.studyEndAt),
    canSignIn,
    signInHint:
      beforeStart && startMs ? formatHmFromMs(startMs) + " 后可签到" : "",
    stateLabel: beforeStart ? "待开始" : stateText(st),
  });
}

module.exports = {
  formatSeatDayForDisplay,
  stateText,
  payLabel,
  parseSlotStartMs,
  formatHmFromMs,
  formatTs,
  bizDateOf,
  enrichForList,
  enrichForDetail,
};
