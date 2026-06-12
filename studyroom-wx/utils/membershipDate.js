/** 期限卡生效日：与后端 study.mini.membership-max-activate-years 一致（默认 2 年） */
const DEFAULT_MAX_ACTIVATE_YEARS = 2;

function pad2(n) {
  return n < 10 ? "0" + n : "" + n;
}

function formatYmd(d) {
  return `${d.getFullYear()}-${pad2(d.getMonth() + 1)}-${pad2(d.getDate())}`;
}

function todayStr() {
  return formatYmd(new Date());
}

function addYearsFromYmd(ymd, years) {
  const parts = String(ymd).slice(0, 10).split("-");
  const d = new Date(
    parseInt(parts[0], 10),
    parseInt(parts[1], 10) - 1,
    parseInt(parts[2], 10)
  );
  d.setFullYear(d.getFullYear() + years);
  return formatYmd(d);
}

function clampYmd(ymd, minYmd, maxYmd) {
  const s = String(ymd).slice(0, 10);
  if (minYmd && s < minYmd) return minYmd;
  if (maxYmd && s > maxYmd) return maxYmd;
  return s;
}

/** 解析后端返回的日期（时间戳毫秒或 yyyy-MM-dd） */
function fromApiDate(v) {
  if (v == null || v === "") return "";
  if (typeof v === "number") {
    return formatYmd(new Date(v));
  }
  const s = String(v).replace("T", " ").trim();
  if (s.length >= 10) return s.slice(0, 10);
  return "";
}

function maxActivateDateStr(years) {
  const y = years > 0 ? years : DEFAULT_MAX_ACTIVATE_YEARS;
  return addYearsFromYmd(todayStr(), y);
}

module.exports = {
  DEFAULT_MAX_ACTIVATE_YEARS,
  todayStr,
  formatYmd,
  addYearsFromYmd,
  clampYmd,
  fromApiDate,
  maxActivateDateStr,
};
