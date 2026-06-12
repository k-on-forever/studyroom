// subpackages/pages/region/index.js
const pages = require("../../../pages/index.js");
const {
  getFloorList,
  getRoomList,
  getSeatList,
} = require("../../../api/region");
const { browseOverlay } = require("../../../api/seatBrowseLock");
const { quoteAppointment } = require("../../../api/application");
const { getReservationRule } = require("../../../api/reservationRule");
const { getBookingConfig } = require("../../../api/bookingConfig");
const { formatHttpError } = require("../../../utils/http");

/** 与后端 sys_reservation_rule.advance_booking_days、TimeSlotCodec 一致 */
let ADVANCE_BOOKING_DAYS = 14;
/** 单次预约最短时长（分钟） */
let MIN_BOOKING_DURATION_MINUTES = 60;
/** 时长文案：如 4小时30分 / 2小时 */
function formatDurationCn(totalMin) {
  const m = Math.max(0, Math.round(totalMin));
  const h = Math.floor(m / 60);
  const r = m % 60;
  if (h > 0 && r > 0) {
    return h + "小时" + r + "分";
  }
  if (h > 0) {
    return h + "小时";
  }
  return r + "分钟";
}
/** 与后端 study.reservation.day-start / day-end 一致 */
let BIZ_START = "08:00";
let BIZ_END = "22:30";
/** 与后端 study.reservation.slot-minutes 一致 */
let SLOT_MINUTES = 10;
let SLOT_STEP_MS = SLOT_MINUTES * 60 * 1000;

function hmFromTotalMinutes(totalMin) {
  const h = Math.floor(totalMin / 60);
  const m = totalMin % 60;
  return (h < 10 ? "0" : "") + h + ":" + (m < 10 ? "0" : "") + m;
}
/** 允许的结束时刻上限（对齐 SLOT_MINUTES，≤ 营业结束） */
function computeLastAllowedEndHm() {
  const ps = BIZ_START.split(":");
  const pe = BIZ_END.split(":");
  const sm = parseInt(ps[0], 10) * 60 + parseInt(ps[1], 10);
  const em = parseInt(pe[0], 10) * 60 + parseInt(pe[1], 10);
  const n = Math.floor((em - sm) / SLOT_MINUTES);
  return hmFromTotalMinutes(sm + n * SLOT_MINUTES);
}
let LAST_END_HM = computeLastAllowedEndHm();

/**
 * 单次预约至少 MIN_BOOKING_DURATION_MINUTES 时，最晚「开始」时刻（对齐 SLOT_MINUTES），须 ≤ LAST_END_HM − 最短时长。
 * 例如营业至 22:30、最少约 10 分钟 → 最晚开始约 22:20（对齐 10 分钟栅格）。
 */
function computeLastStartHmForMinBooking() {
  const ps = BIZ_START.split(":");
  const pe = BIZ_END.split(":");
  const sm = parseInt(ps[0], 10) * 60 + parseInt(ps[1], 10);
  const em = parseInt(pe[0], 10) * 60 + parseInt(pe[1], 10);
  const n = Math.floor((em - sm) / SLOT_MINUTES);
  const lastEndMin = sm + n * SLOT_MINUTES;
  let latestStartMin = lastEndMin - MIN_BOOKING_DURATION_MINUTES;
  if (latestStartMin < sm) {
    return hmFromTotalMinutes(sm);
  }
  const off = latestStartMin - sm;
  latestStartMin = sm + Math.floor(off / SLOT_MINUTES) * SLOT_MINUTES;
  return hmFromTotalMinutes(latestStartMin);
}
let LAST_START_FOR_MIN_BOOKING_HM = computeLastStartHmForMinBooking();

/** 从后端 config 刷新以上全部常量 */
function applyBookingConfig(cfg) {
  if (cfg.slotMinutes != null) SLOT_MINUTES = cfg.slotMinutes;
  if (cfg.dayStart) BIZ_START = cfg.dayStart;
  if (cfg.dayEnd) BIZ_END = cfg.dayEnd;
  if (cfg.advanceBookingDays != null) ADVANCE_BOOKING_DAYS = cfg.advanceBookingDays;
  SLOT_STEP_MS = SLOT_MINUTES * 60 * 1000;
  LAST_END_HM = computeLastAllowedEndHm();
  LAST_START_FOR_MIN_BOOKING_HM = computeLastStartHmForMinBooking();
  const p = String(LAST_START_FOR_MIN_BOOKING_HM).split(":");
  START_CAP_HOUR = parseInt(p[0], 10) || 21;
  START_CAP_MINUTE = parseInt(p[1], 10) || 30;
}

/** 将 yyyy-MM-dd/HH:mm-HH:mm 格式成可读一行 */
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

/** 预约窗口「最后一天」的最晚可「开始」时刻（与 onLoad 里初始 pickerStartMaxDate 一致） */
function pickerMaxEndFromPageMax(maxDateMs) {
  const lastDayMid = new Date(maxDateMs);
  lastDayMid.setHours(0, 0, 0, 0);
  const ls = LAST_START_FOR_MIN_BOOKING_HM.split(":");
  const pickerMaxEnd = new Date(lastDayMid.getTime());
  pickerMaxEnd.setHours(
    parseInt(ls[0], 10) || 21,
    parseInt(ls[1], 10) || 0,
    0,
    0
  );
  return pickerMaxEnd.getTime();
}

let START_CAP_HOUR = 21;
let START_CAP_MINUTE = 30;
(function updateStartCap() {
  const p = String(LAST_START_FOR_MIN_BOOKING_HM).split(":");
  START_CAP_HOUR = parseInt(p[0], 10) || 21;
  START_CAP_MINUTE = parseInt(p[1], 10) || 30;
})();

Page({
  /**
   * 页面的初始数据
   */
  data: {
    roomName: "",
    roomTime: "",
    stateTime: "",
    endTime: "",
    /** 预定时间展示：YYYY-MM-DD HH:mm */
    displayBookingStart: "",
    displayBookingEnd: "",
    /** datetime-picker 绑定值 */
    valueStartMs: 0,
    valueEndMs: 0,
    /** 结束时刻仅可选「开始日期」当天 */
    slotDayMinTs: 0,
    slotDayMaxTs: 0,
    minHour: 10,
    maxHour: 20,
    /** 在 onLoad 里初始化，避免 maxDate 早于今天导致日期选择器空白错乱 */
    minDate: 0,
    maxDate: 0,
    /** 日期选择器 max-date（最后一天 12:00，避免 23:59 在部分机型上日列异常） */
    maxBookDayMs: 0,
    bookingRangeEndLabel: "",
    /** 开始时间：原生 picker-view 三列（日期 / 时 / 分） */
    startPvDates: [],
    startPvDateMsList: [],
    startPvHours: [],
    startPvMinutes: [],
    startPvValue: [0, 0, 0],
    stateCurrentDate: 0,
    endCurrentDate: 0,
    // 时间弹框：开始分「日期 + 时刻」；结束仍为 datetime
    show: false,
    flag: false,
    // 楼层数据
    floor: [],
    floorIndex: 0,
    // 房间
    room: [],
    roomIndex: 0,
    // 座位
    seatList: [],
    /** 三层 × 每層 20 格（未满为占位格） */
    seatMegaMap: null,
    /** 有有效 gridRow/grid_col 时的坐标平面图 */
    seatCoordMap: null,
    /** 座位占用覆盖（正式预约等，与 overlay 接口一致） */
    browseOverlayMap: [],
    seatIndex: -1,
    // 预约信息
    dataFrom: {
      floorId: "",
      roomId: "",
      seatId: "",
      seatDay: "",
    },
    /** 与后端 TimeSlotCodec 一致：同一天 HH:mm-HH:mm，按 SLOT_MINUTES 栅格对齐 */
    slotStart: "09:00",
    slotEnd: "12:00",
    /** 与 ADVANCE_BOOKING_DAYS 一致，供 WXML 提示文案 */
    advanceBookingDays: ADVANCE_BOOKING_DAYS,
    /** 开始时刻：可选的最后一天 22:30（用于 picker max-date 边界） */
    pickerStartMaxDate: 0,
    /** 当前选中的楼层名称（与 item.floor 一致） */
    floorLabel: "",
    /** 长说明折叠：false 仅显示一行摘要 */
    showLongRules: false,
    /** onLoad 写入 min/max 后为 true，供时间弹层 wx:if（避免 0/NaN 与未初始化） */
    bookingPickersReady: false,
    seatLoading: false,
    /** 当日临近打烊提示 */
    closingHint: "",
    /** 选「开始」时可选的最晚小时（须留出最短预约时长，见 LAST_START_FOR_MIN_BOOKING） */
    pickerStartMaxHour: 21,
    /** 选「结束」时可选的最晚小时（与 LAST_END_HM 一致） */
    pickerEndMaxHour: 22,
    /** 今日可选「开始」下限（毫秒）；非今日 -1 */
    pickerSameDayEarliestMs: -1,
    /** 今日可选「结束」下限（毫秒）；非今日 -1 */
    pickerEndSameDayEarliestMs: -1,
    /** 选「开始」时分钟列上限（10 分一档，非 cap 小时一般 50） */
    pickerStartMaxMinute: 50,
    /** 「结束时间」弹层说明：最早结束 = 开始 + 最短时长 */
    pickerEndEarliestText: "",
    /** 按开始时刻动态生成的快捷时长（1小时起，至打烊） */
    durationPresets: [],
    durationPanelHint: "",
    /** 当前选中的快捷时长 key */
    selectedDurationKey: "",
    /** 时间滚轮列后缀：年/月/日/时/分 */
    pickerColumnFormatter(type, value) {
      const units = {
        year: "年",
        month: "月",
        day: "日",
        hour: "时",
        minute: "分",
      };
      return units[type] ? value + units[type] : value;
    },
  },

  onClose() {
    this.setData({
      show: false,
      flag: false,
      pickerSameDayEarliestMs: -1,
      pickerEndSameDayEarliestMs: -1,
      pickerEndEarliestText: "",
    });
  },
  /** 预约最后一天允许的最晚「开始时刻」（须满足单次最短时长） */
  lastSelectableInstantMs() {
    const lastDay0 = this.startOfDayMs(this.data.maxDate);
    return this.combineDaySlotMs(lastDay0, LAST_START_FOR_MIN_BOOKING_HM);
  },
  showPopup() {
    const today0 = this.startOfDayMs(Date.now());
    let day = this.data.stateCurrentDate;
    let day0 = this.startOfDayMs(day);
    if (day0 === today0 && this.nextSlotStartAfterNow(today0, Date.now()) == null) {
      const fut = this.firstBookableFutureDay0(today0, this.data.maxDate);
      if (fut == null) {
        return;
      }
      const nv = this.nextSlotStartAfterNow(fut, Date.now());
      if (nv == null) {
        return;
      }
      const hm = this.extractHm(nv);
      let slotEnd = this.endHmFromDuration(hm, 1, fut) || this.data.slotEnd;
      const endTs = this.clampEndToBiz(
        this.combineDaySlotMs(fut, slotEnd),
        nv
      );
      if (endTs != null) {
        slotEnd = this.extractHm(endTs);
      }
      const dayStr = this.timestampToTime(fut);
      const vEnd = this.combineDaySlotMs(fut, slotEnd);
      this.setData(
        {
          stateCurrentDate: fut,
          endCurrentDate: fut,
          stateTime: dayStr,
          endTime: dayStr,
          slotStart: hm,
          slotEnd,
          selectedDurationKey: "",
          valueStartMs: nv,
          valueEndMs: vEnd,
        },
        () => {
          this.syncBookingDisplays();
          this.refreshBrowseOverlay();
          this.openStartDatetimePickerSheet(fut);
        }
      );
      return;
    }
    this.openStartDatetimePickerSheet(day);
  },

  /** 打开「开始日期时间」五列滚轮（年/月/日/时/分） */
  openStartDatetimePickerSheet(dayMs) {
    if (!this.data.bookingPickersReady) {
      wx.showToast({ title: "页面未就绪，请稍后重试", icon: "none" });
      return;
    }
    const today0 = this.startOfDayMs(Date.now());
    const vs0 = this.combineDaySlotMs(dayMs, this.data.slotStart);
    const vs = this.clampStartToBiz(vs0);
    let valueStartMs = vs != null ? vs : vs0;
    const dayV = this.startOfDayMs(valueStartMs);
    const earliestOnChosenDay = this.nextSlotStartAfterNow(dayV, Date.now());
    if (earliestOnChosenDay != null) {
      valueStartMs = Math.max(valueStartMs, earliestOnChosenDay);
    } else {
      let snap = this.nextSlotStartAfterNow(today0, Date.now());
      if (snap == null) {
        const fut = this.firstBookableFutureDay0(today0, this.data.maxDate);
        if (fut != null) {
          snap = this.nextSlotStartAfterNow(fut, Date.now());
        }
      }
      if (snap != null) {
        valueStartMs = snap;
      }
    }
    let pickerSameDayEarliestMs = -1;
    if (this.startOfDayMs(valueStartMs) === today0) {
      const nx0 = this.nextSlotStartAfterNow(today0, Date.now());
      if (nx0 != null) {
        pickerSameDayEarliestMs = nx0;
      }
    }
    const pv = this.composeStartPickerState(valueStartMs);
    this.setData(
      Object.assign(
        {
          show: true,
          valueStartMs,
          pickerSameDayEarliestMs,
        },
        pv
      )
    );
  },

  buildStartPickerDateList() {
    const minT = this.startOfDayMs(this.data.minDate);
    const maxT = this.startOfDayMs(this.data.maxBookDayMs || this.data.maxDate);
    const today0 = this.startOfDayMs(Date.now());
    const labels = [];
    const msl = [];
    const cursor = new Date(minT);
    const end = new Date(maxT);
    while (cursor.getTime() <= end.getTime()) {
      if (cursor.getTime() === today0 && this.nextSlotStartAfterNow(today0, Date.now()) == null) {
        cursor.setDate(cursor.getDate() + 1);
        continue;
      }
      msl.push(cursor.getTime());
      labels.push(this.timestampToTime(cursor.getTime()));
      cursor.setDate(cursor.getDate() + 1);
    }
    if (!msl.length) {
      const t = today0 + 86400000;
      msl.push(t);
      labels.push(this.timestampToTime(t));
    }
    const result = { labels, msl };
    return result;
  },

  buildStartPickerHours(dayMs) {
    const hours = [];
    const today0 = this.startOfDayMs(Date.now());
    const lastDay0 = this.startOfDayMs(this.data.maxDate);
    let minH = 8;
    if (dayMs === today0) {
      const nx = this.nextSlotStartAfterNow(today0, Date.now());
      if (nx != null) {
        minH = new Date(nx).getHours();
      } else {
        return [];
      }
    }
    let maxH = this.data.pickerStartMaxHour || START_CAP_HOUR;
    if (dayMs === lastDay0) {
      maxH = Math.min(maxH, START_CAP_HOUR);
    }
    for (let h = minH; h <= maxH; h++) {
      hours.push((h < 10 ? "0" : "") + h);
    }
    return hours.length ? hours : ["08"];
  },

  buildStartPickerMinutes(dayMs, hourStr) {
    const h = parseInt(hourStr, 10) || 8;
    const stepMax = 50;
    let maxM =
      h >= START_CAP_HOUR ? Math.min(START_CAP_MINUTE, stepMax) : stepMax;
    const mins = [];
    const today0 = this.startOfDayMs(Date.now());
    let minM = 0;
    if (dayMs === today0) {
      const nx = this.nextSlotStartAfterNow(today0, Date.now());
      if (nx != null) {
        const nd = new Date(nx);
        if (nd.getHours() === h) {
          minM = nd.getMinutes();
          if (minM > maxM) {
            maxM = minM;
          }
        }
      }
    }
    for (let m = 0; m <= maxM; m += SLOT_MINUTES) {
      if (m < minM) {
        continue;
      }
      mins.push((m < 10 ? "0" : "") + m);
    }
    return mins.length ? mins : ["00"];
  },

  composeStartPickerState(anchorMs) {
    const datePack = this.buildStartPickerDateList();
    const dayMs = this.startOfDayMs(anchorMs);
    let dateIdx = datePack.msl.indexOf(dayMs);
    if (dateIdx < 0) {
      dateIdx = 0;
    }
    const hm = this.extractHm(anchorMs);
    const hp = hm.split(":");
    const hours = this.buildStartPickerHours(datePack.msl[dateIdx]);
    let hourIdx = hours.indexOf(hp[0]);
    if (hourIdx < 0) {
      hourIdx = 0;
    }
    const minutes = this.buildStartPickerMinutes(
      datePack.msl[dateIdx],
      hours[hourIdx]
    );
    let minIdx = minutes.indexOf(hp[1]);
    if (minIdx < 0) {
      minIdx = 0;
    }
    return {
      startPvDates: datePack.labels,
      startPvDateMsList: datePack.msl,
      startPvHours: hours,
      startPvMinutes: minutes,
      startPvValue: [dateIdx, hourIdx, minIdx],
    };
  },

  onStartPickerViewChange(e) {
    this.onStartPickerViewChangeImpl(e);
  },
  onStartPickerViewChangeImpl(e) {
    const val = e.detail.value || [0, 0, 0];
    const di = val[0] || 0;
    const prev = this.data.startPvValue || [0, 0, 0];
    const msl = this.data.startPvDateMsList || [];
    const dayMs = msl[di];
    if (!dayMs) {
      return;
    }
    let hi = val[1] || 0;
    let mi = val[2] || 0;
    let hours = this.data.startPvHours || [];
    let minutes = this.data.startPvMinutes || [];
    if (di !== prev[0]) {
      hours = this.buildStartPickerHours(dayMs);
      hi = 0;
      mi = 0;
    }
    const hourStr = hours[hi] || hours[0] || "08";
    if (di !== prev[0] || hi !== prev[1]) {
      minutes = this.buildStartPickerMinutes(dayMs, hourStr);
      if (hi !== prev[1]) {
        mi = 0;
      }
    }
    if (hi >= hours.length) {
      hi = Math.max(0, hours.length - 1);
    }
    if (mi >= minutes.length) {
      mi = Math.max(0, minutes.length - 1);
    }
    this.setData({
      startPvHours: hours,
      startPvMinutes: minutes,
      startPvValue: [di, hi, mi],
    });
  },

  onConfirmStartPicker() {
    const v = this.data.startPvValue || [0, 0, 0];
    const dayMs = (this.data.startPvDateMsList || [])[v[0]];
    const h = (this.data.startPvHours || [])[v[1]] || "08";
    const m = (this.data.startPvMinutes || [])[v[2]] || "00";
    if (!dayMs) {
      wx.showToast({ title: "请选择日期", icon: "none" });
      return;
    }
    const combined = this.combineDaySlotMs(dayMs, h + ":" + m);
    const ok = this.clampStartToBiz(combined);
    if (ok == null) {
      wx.showToast({
        title:
          "该时刻不可约：须满足营业时间且单次至少 " +
          MIN_BOOKING_DURATION_MINUTES / 60 +
          " 小时；当日最晚开始 " +
          LAST_START_FOR_MIN_BOOKING_HM,
        icon: "none",
        duration: 3200,
      });
      return;
    }
    this.applyStartSelection(ok);
  },

  applyStartSelection(ts) {
    const day0 = this.startOfDayMs(ts);
    const hm = this.extractHm(ts);
    const startMs = this.combineDaySlotMs(day0, hm);
    let slotEnd = this.endHmFromDuration(hm, 1, day0);
    if (!slotEnd) {
      const minEnd = this.clampEndToBiz(
        startMs + MIN_BOOKING_DURATION_MINUTES * 60 * 1000,
        startMs
      );
      slotEnd = minEnd != null ? this.extractHm(minEnd) : this.data.slotEnd;
    }
    const endTs = this.clampEndToBiz(
      this.combineDaySlotMs(day0, slotEnd),
      startMs
    );
    if (endTs != null) {
      slotEnd = this.extractHm(endTs);
    }
    const dayStr = this.timestampToTime(day0);
    this.setData(
      {
        stateCurrentDate: day0,
        endCurrentDate: day0,
        stateTime: dayStr,
        endTime: dayStr,
        slotStart: hm,
        slotEnd,
        show: false,
        valueStartMs: ts,
        valueEndMs: this.combineDaySlotMs(day0, slotEnd),
        selectedDurationKey: "",
      },
      () => {
        this.syncBookingDisplays();
        this.refreshBrowseOverlay();
      }
    );
  },

  /** 开始时间： cap 小时已为 START_CAP_HOUR；cap 小时内分钟不超过 START_CAP_MINUTE；其余小时不超过 50（10 分一档） */
  updatePickerStartMinuteCap(tsMs) {
    const d = new Date(tsMs);
    const stepMax = 50;
    const maxMin =
      d.getHours() >= START_CAP_HOUR ? Math.min(START_CAP_MINUTE, stepMax) : stepMax;
    if (maxMin !== this.data.pickerStartMaxMinute) {
      this.setData({ pickerStartMaxMinute: maxMin });
    }
  },

  /** 滚轮变更：今日则同步「最早可约」下限 */
  /** 结束时间滚轮：今日则下限不低于 max(最小结束时刻, 此刻) */
  onEndPickerInput(e) {
    const raw = e.detail;
    const tsNum = typeof raw === "number" ? raw : Number(raw);
    if (Number.isNaN(tsNum)) {
      return;
    }
    const today0 = this.startOfDayMs(Date.now());
    const d0 = this.startOfDayMs(tsNum);
    let earliestMs = -1;
    if (d0 === today0 && this.data.slotDayMinTs > 0) {
      const stepMs = SLOT_MINUTES * 60 * 1000;
      const rawEnd = Math.max(this.data.slotDayMinTs, Date.now());
      earliestMs = Math.ceil(rawEnd / stepMs) * stepMs;
    }
    if (earliestMs !== this.data.pickerEndSameDayEarliestMs) {
      this.setData({ pickerEndSameDayEarliestMs: earliestMs });
    }
  },

  showPopupEnd() {
    const day0 = this.startOfDayMs(this.data.stateCurrentDate);
    const sm = this.combineDaySlotMs(day0, this.data.slotStart);
    const dayMaxEnd = this.combineDaySlotMs(day0, LAST_END_HM);
    let dayMin = sm + MIN_BOOKING_DURATION_MINUTES * 60 * 1000;
    if (dayMin > dayMaxEnd) {
      wx.showToast({
        title:
          "单次至少 " +
          MIN_BOOKING_DURATION_MINUTES / 60 +
          " 小时：开始不得晚于 " +
          LAST_START_FOR_MIN_BOOKING_HM +
          "（营业结束 " +
          LAST_END_HM +
          "）",
        icon: "none",
        duration: 3200,
      });
      return;
    }
    const ve0 = this.combineDaySlotMs(day0, this.data.slotEnd);
    let ve = this.clampEndToBiz(ve0, sm);
    const today0 = this.startOfDayMs(Date.now());
    let pickerEndSameDayEarliestMs = -1;
    if (day0 === today0) {
      const stepMs = SLOT_MINUTES * 60 * 1000;
      const rawEnd = Math.max(dayMin, Date.now());
      pickerEndSameDayEarliestMs = Math.ceil(rawEnd / stepMs) * stepMs;
    }
    const pickerEndEarliestText =
      "最早可选结束 " +
      this.timestampToTime(day0) +
      " " +
      this.extractHm(dayMin) +
      "（须晚于开始至少 " +
      MIN_BOOKING_DURATION_MINUTES / 60 +
      " 小时）";
    this.setData({
      flag: true,
      valueEndMs: ve != null ? ve : ve0,
      slotDayMinTs: dayMin,
      slotDayMaxTs: dayMaxEnd,
      pickerEndSameDayEarliestMs,
      pickerEndEarliestText,
    });
  },
  /** 仅日期 YYYY-MM-DD（用于与后端 seatDay 日期部分一致） */
  timestampToTime(timestamp) {
    const date = new Date(timestamp);
    const Y = date.getFullYear() + "-";
    const M =
      (date.getMonth() + 1 < 10
        ? "0" + (date.getMonth() + 1)
        : date.getMonth() + 1) + "-";
    const D = date.getDate() < 10 ? "0" + date.getDate() : date.getDate();
    return Y + M + D;
  },

  startOfDayMs(ts) {
    const d = new Date(ts);
    d.setHours(0, 0, 0, 0);
    return d.getTime();
  },
  endOfDayMs(ts) {
    const d = new Date(ts);
    d.setHours(23, 59, 59, 999);
    return d.getTime();
  },
  combineDaySlotMs(dayTs, hm) {
    const d = new Date(dayTs);
    d.setHours(0, 0, 0, 0);
    const p = (hm || "09:00").split(":");
    const hh = parseInt(p[0], 10) || 0;
    const mm = parseInt(p[1], 10) || 0;
    d.setHours(hh, mm, 0, 0);
    return d.getTime();
  },
  /**
   * 某日营业栅格内「当前仍可选」的最早整点开始时刻（与后端当日策略一致）：
   * - 若 now 落在某一档 [t, t+槽长) 内，仍返回该档起点 t（例如 21:05 在 21:00–22:00 内仍可选 21:00 开始）；
   * 否则返回严格晚于 now 的第一档起点。
   */
  nextSlotStartAfterNow(day0, nowMs) {
    const bizStart = this.combineDaySlotMs(day0, BIZ_START);
    const lastStart = this.combineDaySlotMs(day0, LAST_START_FOR_MIN_BOOKING_HM);
    for (let t = bizStart; t <= lastStart; t += SLOT_STEP_MS) {
      if (t > nowMs) {
        return t;
      }
    }
    return null;
  },

  /** 从「明天」起在 max 范围内找第一个仍有可约开始时刻的日历日 0 点（86400000 步进） */
  firstBookableFutureDay0(today0, maxTEndMs) {
    let d = this.startOfDayMs(today0) + 86400000;
    const cap = this.startOfDayMs(maxTEndMs);
    while (d <= cap) {
      if (this.nextSlotStartAfterNow(d, Date.now()) != null) {
        return d;
      }
      d += 86400000;
    }
    return null;
  },

  /** 向下对齐到营业栅格（SLOT_MINUTES），并夹在「开始～最晚可开始」之间 */
  snapToHour(ts) {
    const d = new Date(typeof ts === "number" ? ts : Number(ts));
    d.setSeconds(0, 0);
    d.setMilliseconds(0);
    const day0 = this.startOfDayMs(ts);
    const floor = this.combineDaySlotMs(day0, BIZ_START);
    let offMin = Math.floor((d.getTime() - floor) / 60000);
    if (offMin < 0) offMin = 0;
    let snapped = Math.floor(offMin / SLOT_MINUTES) * SLOT_MINUTES;
    let t = floor + snapped * 60000;
    if (t < floor) t = floor;
    return t;
  },

  /** 开始时刻：须在预约日内、营业时间内；当日不可早于当前仍可选的最早栅格 */
  clampStartToBiz(ts) {
    let t = this.snapToHour(typeof ts === "number" ? ts : Number(ts));
    const day0 = this.startOfDayMs(t);
    const minDay = this.startOfDayMs(this.data.minDate);
    const maxDay = this.startOfDayMs(this.data.maxDate);
    if (day0 < minDay || day0 > maxDay) return null;

    const bizStart = this.combineDaySlotMs(day0, BIZ_START);
    const lastStart = this.combineDaySlotMs(day0, LAST_START_FOR_MIN_BOOKING_HM);

    if (t < bizStart) t = bizStart;

    const today0 = this.startOfDayMs(Date.now());
    if (day0 === today0) {
      const earliest = this.nextSlotStartAfterNow(day0, Date.now());
      if (earliest == null) {
        return null;
      }
      if (t < earliest) {
        t = earliest;
      }
      if (t > lastStart) {
        return null;
      }
    } else {
      if (t > lastStart) t = lastStart;
    }
    return t;
  },

  /** 结束时刻：同一天、不晚于 LAST_END_HM、且晚于开始至少 MIN_BOOKING_DURATION_MINUTES */
  clampEndToBiz(ts, startMs) {
    let t = this.snapToHour(typeof ts === "number" ? ts : Number(ts));
    const day0 = this.startOfDayMs(t);
    const cap = this.combineDaySlotMs(day0, LAST_END_HM);
    const minSpanMs = MIN_BOOKING_DURATION_MINUTES * 60 * 1000;
    const minEnd = startMs != null ? startMs + minSpanMs : null;
    if (minEnd != null && minEnd > cap) {
      return null;
    }
    const lo =
      minEnd != null
        ? minEnd
        : this.combineDaySlotMs(day0, BIZ_START) + MIN_BOOKING_DURATION_MINUTES * 60 * 1000;

    if (t < lo) t = lo;
    if (t > cap) t = cap;
    if (startMs != null && t <= startMs) return null;
    return t;
  },
  extractHm(ts) {
    const d = new Date(ts);
    const h = d.getHours() < 10 ? "0" + d.getHours() : "" + d.getHours();
    const m = d.getMinutes() < 10 ? "0" + d.getMinutes() : "" + d.getMinutes();
    return h + ":" + m;
  },
  syncBookingDisplays() {
    const d = (this.data.stateTime || "").substr(0, 10);
    if (d.length !== 10) return;
    const presets = this.buildDurationPresets(
      this.data.slotStart,
      this.data.stateCurrentDate
    );
    let selectedKey = this.data.selectedDurationKey;
    const matched = this.matchDurationPresetKey(
      this.data.slotStart,
      this.data.slotEnd,
      this.data.stateCurrentDate,
      presets
    );
    if (matched) {
      selectedKey = matched;
    } else {
      selectedKey = "";
    }
    const panelHint =
      presets.length > 0
        ? "可选时长按当前开始时刻至营业 " + BIZ_END + " 自动计算"
        : "";
    this.setData({
      displayBookingStart: d + " " + this.data.slotStart,
      displayBookingEnd: d + " " + this.data.slotEnd,
      durationPresets: presets,
      selectedDurationKey: selectedKey,
      durationPanelHint: panelHint,
    });
    this.updateClosingHint();
  },

  syncFloorLabel() {
    const floors = this.data.floor || [];
    const idx = this.data.floorIndex;
    const name = floors[idx] && floors[idx].floor ? floors[idx].floor : "";
    this.setData({ floorLabel: name });
  },

  /** 预约日为今天且距营业结束较近时给出提示 */
  updateClosingHint() {
    const d = (this.data.stateTime || "").substr(0, 10);
    if (d.length !== 10) {
      this.setData({ closingHint: "" });
      return;
    }
    const parts = d.split("-");
    const pick0 = new Date(
      parseInt(parts[0], 10),
      parseInt(parts[1], 10) - 1,
      parseInt(parts[2], 10)
    ).setHours(0, 0, 0, 0);
    const now = new Date();
    const today0 = new Date(
      now.getFullYear(),
      now.getMonth(),
      now.getDate()
    ).setHours(0, 0, 0, 0);
    if (pick0 !== today0) {
      this.setData({ closingHint: "" });
      return;
    }
    const endParts = BIZ_END.split(":");
    const endH = parseInt(endParts[0], 10) || 22;
    const endM = parseInt(endParts[1], 10) || 30;
    const endTs = new Date(
      now.getFullYear(),
      now.getMonth(),
      now.getDate(),
      endH,
      endM,
      0
    ).getTime();
    const minsLeft = (endTs - now.getTime()) / 60000;
    if (minsLeft <= 0) {
      this.setData({
        closingHint:
          "今日营业已结束。仍可预约明日及之后 14 日内时段，请点「预定时间」改选日期",
      });
    } else if (minsLeft <= 120) {
      this.setData({
        closingHint:
          "今日距打烊不足 " +
          Math.max(1, Math.round(minsLeft)) +
          " 分钟，可选连续时段较紧",
      });
    } else {
      this.setData({ closingHint: "" });
    }
  },

  toggleLongRules() {
    this.setData({ showLongRules: !this.data.showLongRules });
  },

  /**
   * 从开始时刻到打烊可约时长（分钟，已对齐栅格）
   */
  minutesUntilClose(startHm, dayMs) {
    if (!startHm) {
      return 0;
    }
    const day0 =
      dayMs != null
        ? this.startOfDayMs(dayMs)
        : this.startOfDayMs(this.data.stateCurrentDate);
    const startMs = this.combineDaySlotMs(day0, startHm);
    const closeHm = this.endHmFromDuration(startHm, -1, day0);
    if (!closeHm) {
      return 0;
    }
    return Math.max(0, this.timeToMinutes(closeHm) - this.timeToMinutes(startHm));
  },

  /** 动态快捷时长：1小时、2小时…直至整小时上限，另附「到打烊(实际时长)」 */
  buildDurationPresets(startHm, dayMs) {
    if (!startHm) {
      return [];
    }
    const day0 =
      dayMs != null
        ? this.startOfDayMs(dayMs)
        : this.startOfDayMs(this.data.stateCurrentDate);
    const availMin = this.minutesUntilClose(startHm, day0);
    if (availMin < MIN_BOOKING_DURATION_MINUTES) {
      return [];
    }
    const sm = this.timeToMinutes(startHm);
    const presets = [];
    const maxWholeHours = Math.floor(availMin / 60);
    for (let h = 1; h <= maxWholeHours; h++) {
      const endHm = this.endHmFromDuration(startHm, h, day0);
      if (!endHm) {
        continue;
      }
      const dur = this.timeToMinutes(endHm) - sm;
      if (dur < MIN_BOOKING_DURATION_MINUTES) {
        continue;
      }
      presets.push({
        key: "h" + h,
        label: h + "小时",
        hours: h,
      });
    }
    const lastWholeMin =
      maxWholeHours >= 1 ? maxWholeHours * 60 : 0;
    if (availMin > lastWholeMin) {
      presets.push({
        key: "close",
        label: "到打烊",
        subLabel: formatDurationCn(availMin),
        hours: -1,
        durationMinutes: availMin,
      });
    }
    return presets;
  },

  /** 根据开始时刻与快捷时长计算结束 HH:mm（对齐 10 分钟栅格并夹在营业内） */
  endHmFromDuration(startHm, hours, dayMs) {
    const day0 =
      dayMs != null
        ? this.startOfDayMs(dayMs)
        : this.startOfDayMs(this.data.stateCurrentDate);
    const startMs = this.combineDaySlotMs(day0, startHm);
    let targetMs;
    if (hours < 0) {
      targetMs = this.combineDaySlotMs(day0, LAST_END_HM);
    } else {
      targetMs = startMs + hours * 60 * 60 * 1000;
    }
    const ok = this.clampEndToBiz(targetMs, startMs);
    return ok != null ? this.extractHm(ok) : null;
  },

  matchDurationPresetKey(startHm, endHm, dayMs, presetList) {
    const sm = this.timeToMinutes(startHm);
    const em = this.timeToMinutes(endHm);
    if (em <= sm) {
      return "";
    }
    const list =
      presetList ||
      this.buildDurationPresets(startHm, dayMs != null ? dayMs : this.data.stateCurrentDate);
    for (let i = 0; i < list.length; i++) {
      const p = list[i];
      const eh = this.endHmFromDuration(
        startHm,
        p.hours,
        dayMs != null ? dayMs : this.data.stateCurrentDate
      );
      if (eh && eh === endHm) {
        return p.key;
      }
    }
    return "";
  },

  onDurationPresetTap(e) {
    const hours = Number(e.currentTarget.dataset.hours);
    const key = e.currentTarget.dataset.key || "";
    const startHm = this.data.slotStart;
    if (!startHm) {
      wx.showToast({ title: "请先选择开始时间", icon: "none" });
      return;
    }
    const endHm = this.endHmFromDuration(
      startHm,
      hours,
      this.data.stateCurrentDate
    );
    if (!endHm) {
      wx.showToast({
        title:
          "该时长不可选：须至少 " +
          MIN_BOOKING_DURATION_MINUTES / 60 +
          " 小时且不超过营业结束",
        icon: "none",
        duration: 2800,
      });
      return;
    }
    const sm = this.timeToMinutes(startHm);
    const em = this.timeToMinutes(endHm);
    if (em - sm < MIN_BOOKING_DURATION_MINUTES) {
      wx.showToast({
        title: "单次预约最短 " + MIN_BOOKING_DURATION_MINUTES / 60 + " 小时",
        icon: "none",
      });
      return;
    }
    const day0 = this.startOfDayMs(this.data.stateCurrentDate);
    this.setData(
      {
        slotEnd: endHm,
        selectedDurationKey: key,
        valueEndMs: this.combineDaySlotMs(day0, endHm),
      },
      () => {
        this.syncBookingDisplays();
        this.refreshBrowseOverlay();
      }
    );
  },
  onConfirmDatetimeEnd(e) {
    const raw = e.detail;
    const tsNum = typeof raw === "number" ? raw : Number(raw);
    const dayStr = this.timestampToTime(this.startOfDayMs(tsNum));
    const startDay = (this.data.stateTime || "").substr(0, 10);
    if (dayStr !== startDay) {
      wx.showToast({ title: "须与开始同一天", icon: "none" });
      return;
    }
    const day0 = this.startOfDayMs(tsNum);
    const startMs = this.combineDaySlotMs(day0, this.data.slotStart);
    const ok = this.clampEndToBiz(tsNum, startMs);
    if (ok == null) {
      wx.showToast({
        title:
          "结束须晚于开始至少 " +
          MIN_BOOKING_DURATION_MINUTES / 60 +
          " 小时，且不晚于 " +
          LAST_END_HM,
        icon: "none",
        duration: 2800,
      });
      return;
    }
    const hm = this.extractHm(ok);
    this.setData({ slotEnd: hm, flag: false, selectedDurationKey: "" }, () => {
      this.syncBookingDisplays();
      this.refreshBrowseOverlay();
    });
  },

  /** 预约/询价失败：含占用时段时用弹窗，避免 toast 截断 */
  showBookFail(msg) {
    const text = (msg && String(msg).trim()) || "操作失败";
    if (text.length > 18 || /占用时段|至\s*\d{2}:\d{2}/.test(text)) {
      wx.showModal({ title: "无法预约", content: text, showCancel: false });
      return;
    }
    wx.showToast({ title: text, icon: "none", duration: 2800 });
  },

  /** 占用时段文案：优先接口 occupancyDetail，否则合并各来源 */
  resolveOccupancyDetail(item) {
    const raw =
      (item && item.occupancyDetail) ||
      [item && item.otherBrowseSeatDay, item && item.busySeatDay, item && item.accessBlockSeatDay]
        .filter(Boolean)
        .join("；");
    const fmt = formatSeatDayForDisplay(raw);
    if (fmt) {
      return fmt;
    }
    const d = (this.data.stateTime || "").substr(0, 10);
    if (d.length === 10 && this.data.slotStart && this.data.slotEnd) {
      return (
        d +
        " " +
        this.data.slotStart +
        " 至 " +
        this.data.slotEnd +
        "（与您当前选择重叠）"
      );
    }
    return "";
  },

  mergeBrowseOverlay(seatList, overlayList) {
    const by = {};
    (overlayList || []).forEach((o) => {
      if (o && o.seatId != null) {
        by[String(o.seatId)] = {
          ruleBlocked: !!o.lockedByOther,
          appointmentBooked: !!o.appointmentBooked,
          appointmentMine: !!o.appointmentMine,
          busySeatDay: o.busySeatDay != null ? String(o.busySeatDay) : "",
          otherBrowseSeatDay:
            o.otherBrowseSeatDay != null ? String(o.otherBrowseSeatDay) : "",
          accessBlockSeatDay:
            o.accessBlockSeatDay != null ? String(o.accessBlockSeatDay) : "",
          occupancyDetail:
            o.occupancyDetail != null ? String(o.occupancyDetail) : "",
        };
      }
    });
    return (seatList || []).map((s) => {
      const id = String(s.seatId != null ? s.seatId : s.id);
      const o = by[id] || {};
      return Object.assign({}, s, {
        ruleBlocked: !!o.ruleBlocked,
        appointmentBooked: !!o.appointmentBooked,
        appointmentMine: !!o.appointmentMine,
        busySeatDay: o.busySeatDay || "",
        otherBrowseSeatDay: o.otherBrowseSeatDay || "",
        accessBlockSeatDay: o.accessBlockSeatDay || "",
        occupancyDetail: o.occupancyDetail || "",
      });
    });
  },

  buildCoordSeatMap(seatList) {
    const list = seatList || [];
    if (!list.length) {
      return null;
    }
    const ok = list.filter(
      (s) => (Number(s.gridRow) || 0) > 0 && (Number(s.gridCol) || 0) > 0
    );
    if (ok.length < list.length * 0.5) {
      return null;
    }
    let maxR = 0;
    let maxC = 0;
    list.forEach((s) => {
      const r = Number(s.gridRow) || 0;
      const c = Number(s.gridCol) || 0;
      if (r > maxR) maxR = r;
      if (c > maxC) maxC = c;
    });
    if (maxR < 1 || maxC < 1) {
      return null;
    }
    const cells = [];
    for (let r = 1; r <= maxR; r++) {
      for (let c = 1; c <= maxC; c++) {
        const seat = list.find(
          (s) => (Number(s.gridRow) || 0) === r && (Number(s.gridCol) || 0) === c
        );
        cells.push({
          key: r + "-" + c,
          ghost: !seat,
          sid: seat ? String(seat.id) : "",
          label: seat ? seat.seatName || String(seat.id) : "",
          locked: seat ? seat.locked : 0,
          roomState: seat ? seat.roomState : "",
          ruleBlocked: seat ? !!seat.ruleBlocked : false,
          appointmentBooked: seat ? !!seat.appointmentBooked : false,
          appointmentMine: seat ? !!seat.appointmentMine : false,
          busySeatDay: seat && seat.busySeatDay ? String(seat.busySeatDay) : "",
          otherBrowseSeatDay:
            seat && seat.otherBrowseSeatDay ? String(seat.otherBrowseSeatDay) : "",
          accessBlockSeatDay:
            seat && seat.accessBlockSeatDay ? String(seat.accessBlockSeatDay) : "",
          occupancyDetail:
            seat && seat.occupancyDetail ? String(seat.occupancyDetail) : "",
          seatType: seat && seat.seatType != null ? seat.seatType : 0,
        });
      }
    }
    return {
      maxR,
      maxC,
      gridStyle: "grid-template-columns: repeat(" + maxC + ", 1fr);",
      cells,
    };
  },

  buildMegaFloorMap(seatList) {
    const list = (seatList || []).slice(0, 60);
    const per = 20;
    const themes = ["t3", "t2", "t1"];
    const meta = [
      {
        title: "三楼 · 沉浸自习",
        badgeLine: "三楼",
        zone: "沉浸区",
        decors: [
          { t: "落地玻璃侧", cls: "tl" },
          { t: "过道", cls: "br" },
        ],
      },
      {
        title: "二楼 · 帘子静音",
        badgeLine: "二楼",
        zone: "帘子间",
        decors: [
          { t: "↑ 入口方向", cls: "tc" },
          { t: "静音区", cls: "bl" },
        ],
      },
      {
        title: "一楼 · 公共自习",
        badgeLine: "一楼",
        zone: "公共区",
        decors: [
          { t: "茶水间方向", cls: "tr" },
          { t: "长桌区", cls: "tl2" },
        ],
      },
    ];
    const tiers = [];
    for (let t = 0; t < 3; t++) {
      const cells = [];
      for (let k = 0; k < per; k++) {
        const seat = list[t * per + k] || null;
        cells.push({
          ghost: !seat,
          sid: seat ? String(seat.id) : "",
          label: seat ? seat.seatName || String(seat.id) : "",
          locked: seat ? seat.locked : 0,
          roomState: seat ? seat.roomState : "",
          ruleBlocked: seat ? !!seat.ruleBlocked : false,
          appointmentBooked: seat ? !!seat.appointmentBooked : false,
          appointmentMine: seat ? !!seat.appointmentMine : false,
          busySeatDay: seat && seat.busySeatDay ? String(seat.busySeatDay) : "",
          otherBrowseSeatDay:
            seat && seat.otherBrowseSeatDay ? String(seat.otherBrowseSeatDay) : "",
          accessBlockSeatDay:
            seat && seat.accessBlockSeatDay ? String(seat.accessBlockSeatDay) : "",
          occupancyDetail:
            seat && seat.occupancyDetail ? String(seat.occupancyDetail) : "",
        });
      }
      tiers.push(Object.assign({}, meta[t], { theme: themes[t], cells }));
    }
    return { tiers };
  },

  timeToMinutes(t) {
    const p = (t || "").split(":");
    if (p.length < 2) return 0;
    return parseInt(p[0], 10) * 60 + parseInt(p[1], 10);
  },

  /** yyyy-MM-dd 转当日 0 点本地时间戳 */
  parseLocalYmd(ymd) {
    const a = (ymd || "").split("-");
    if (a.length !== 3) return NaN;
    const y = parseInt(a[0], 10);
    const m = parseInt(a[1], 10) - 1;
    const d = parseInt(a[2], 10);
    const dt = new Date(y, m, d);
    dt.setHours(0, 0, 0, 0);
    return dt.getTime();
  },

  /** 按提前预约天数初始化 min/max 与默认时段（与后端 TimeSlotCodec 一致） */
  initBookingDateRange(advanceDays) {
    const days =
      advanceDays != null && !Number.isNaN(Number(advanceDays))
        ? Math.max(0, parseInt(advanceDays, 10))
        : ADVANCE_BOOKING_DAYS;
    const today = new Date();
    today.setHours(0, 0, 0, 0);
    const minT = today.getTime();
    const maxCal = new Date(today);
    maxCal.setDate(maxCal.getDate() + days);
    maxCal.setHours(23, 59, 59, 999);
    const maxT = maxCal.getTime();
    const maxBookDay = new Date(today);
    maxBookDay.setDate(maxBookDay.getDate() + days);
    maxBookDay.setHours(12, 0, 0, 0);
    const maxBookDayMs = maxBookDay.getTime();
    const bookingRangeEndLabel = this.timestampToTime(maxBookDayMs);
    const lastDayMid = new Date(maxT);
    lastDayMid.setHours(0, 0, 0, 0);
    const ls = LAST_START_FOR_MIN_BOOKING_HM.split(":");
    const pickerMaxEnd = new Date(lastDayMid.getTime());
    pickerMaxEnd.setHours(parseInt(ls[0], 10) || 21, parseInt(ls[1], 10) || 0, 0, 0);
    this.setData(
      {
        bookingPickersReady: true,
        advanceBookingDays: days,
        minDate: minT,
        maxDate: maxT,
        maxBookDayMs,
        bookingRangeEndLabel,
        pickerStartMaxDate: pickerMaxEnd.getTime(),
        pickerStartMaxHour: START_CAP_HOUR,
        pickerEndMaxHour: parseInt(LAST_END_HM.split(":")[0], 10) || 22,
        stateCurrentDate: minT,
        endCurrentDate: minT,
        stateTime: this.timestampToTime(minT),
        endTime: this.timestampToTime(minT),
        valueStartMs: this.combineDaySlotMs(minT, "09:00"),
        valueEndMs: this.combineDaySlotMs(minT, "10:00"),
      },
      () => {
        const maxD = this.lastSelectableInstantMs();
        let vStart = this.clampStartToBiz(
          this.combineDaySlotMs(minT, "09:00")
        );
        if (vStart == null) {
          const fut = this.firstBookableFutureDay0(minT, maxT);
          if (fut != null) {
            vStart = this.nextSlotStartAfterNow(fut, Date.now());
          }
        }
        if (vStart == null) {
          vStart = this.combineDaySlotMs(minT, BIZ_START);
        }
        const day0 = this.startOfDayMs(vStart);
        const hm = this.extractHm(vStart);
        let slotEnd = this.endHmFromDuration(hm, 1, day0) || "10:00";
        const startMs = vStart;
        const endTs = this.clampEndToBiz(
          this.combineDaySlotMs(day0, slotEnd),
          startMs
        );
        if (endTs != null) {
          slotEnd = this.extractHm(endTs);
        }
        const vEnd = this.combineDaySlotMs(day0, slotEnd);
        const dayStr = this.timestampToTime(day0);
        this.setData(
          {
            pickerStartMaxDate: maxD,
            valueStartMs: vStart,
            valueEndMs: vEnd,
            stateCurrentDate: day0,
            endCurrentDate: day0,
            stateTime: dayStr,
            endTime: dayStr,
            slotStart: hm,
            slotEnd,
            selectedDurationKey: "",
          },
          () => this.syncBookingDisplays()
        );
      }
    );
  },

  /**
   * 生命周期函数--监听页面加载
   */
  onLoad(options) {
    this.loadBookingDateRange();
    this.getFloor();
  },

  onShow() {
    this.loadBookingDateRange();
  },

  loadBookingDateRange() {
    if (this._bookingRangeLoading) {
      return;
    }
    this._bookingRangeLoading = true;
    Promise.all([
      getReservationRule(),
      getBookingConfig()
    ])
      .then(([ruleRes, cfgRes]) => {
        // 先从后端拉取配置同步到模块常量
        if (cfgRes && cfgRes.code === 0 && cfgRes.data) {
          applyBookingConfig(cfgRes.data);
        }
        const d = ruleRes && ruleRes.code === 0 && ruleRes.data
          ? ruleRes.data.advanceBookingDays : null;
        this.initBookingDateRange(d);
      })
      .catch(() => {
        this.initBookingDateRange(ADVANCE_BOOKING_DAYS);
      })
      .finally(() => {
        this._bookingRangeLoading = false;
      });
  },

  // 选择楼层
  reserveFloor(row) {
    const ds = row.currentTarget.dataset;
    const floors = this.data.floor || [];
    const fl =
      floors[ds.index] && floors[ds.index].floor ? floors[ds.index].floor : "";
    this.setData({
      floorIndex: ds.index,
      "dataFrom.floorId": ds.id,
      floorLabel: fl,
    });
    this.getRoom();
  },
  // 获取楼层
  getFloor() {
    getFloorList()
      .then((info) => {
        if (info.code == 0) {
          const list = info.data || [];
          this.setData({
            floor: list,
            floorIndex: 0,
            "dataFrom.floorId": list[0]?.id || "",
            floorLabel: list[0]?.floor || "",
          });
          this.getRoom();
        } else if (info.code === 401) {
          wx.showToast({ title: info.msg || "请先登录", icon: "none" });
        } else {
          wx.showToast({ title: info.msg || "楼层加载失败", icon: "none" });
        }
      })
      .catch((e) => {
        wx.showToast({
          title: formatHttpError(e, "网络异常"),
          icon: "none",
          duration: 3200,
        });
      });
  },

  // 选择自习室
  reserveRoom(row) {
    const ds = row.currentTarget.dataset;
    const item = ds.item || {};
    this.setData({
      roomIndex: ds.index,
      "dataFrom.roomId": ds.id,
      roomName: item.roomName,
      roomTime:
        (item.openingTime || "--") + " · " + (item.closeTime || "--"),
    });
    this.getSeat();
  },
  // 获取自习室
  getRoom() {
    getRoomList(this.data.dataFrom.floorId)
      .then((info) => {
        if (info.code == 0) {
          const rooms = info.data || [];
          this.syncFloorLabel();
          this.setData({
            room: rooms,
            roomName: rooms[0]?.roomName,
            "dataFrom.roomId": rooms[0]?.roomId || "",
            roomTime:
              (rooms[0]?.openingTime || "--") +
              " · " +
              (rooms[0]?.closeTime || "--"),
            roomIndex: 0,
          });
          this.getSeat();
        } else if (info.code === 401) {
          wx.showToast({ title: info.msg || "请先登录", icon: "none" });
        } else {
          wx.showToast({ title: info.msg || "房间加载失败", icon: "none" });
        }
      })
      .catch((e) => {
        wx.showToast({
          title: formatHttpError(e, "网络异常"),
          icon: "none",
          duration: 3200,
        });
      });
  },

  onMegaCellTap(e) {
    const sid = e.currentTarget.dataset.sid;
    if (!sid) {
      return;
    }
    const item = (this.data.seatList || []).find((s) => String(s.id) === String(sid));
    if (!item) return;
    this.handSeat({ currentTarget: { dataset: { item } } });
  },

  // 座位选择
  handSeat(row) {
    const item = row.currentTarget.dataset.item || {};
    const list = this.data.seatList || [];
    const idx = list.findIndex(
      (s) =>
        String(s.id) === String(item.id) ||
        String(s.seatId) === String(item.seatId)
    );
    if (idx < 0) return;
    const seatLabel = item.seatName || item.id || "";
    if (item.appointmentMine) {
      const detail =
        this.resolveOccupancyDetail(item) || "与当前所选时段重叠";
      wx.showModal({
        title: "您已预约本座位",
        content:
          (seatLabel ? "座位 " + seatLabel + "\n占用时段：\n" : "占用时段：\n") +
          detail,
        showCancel: false,
      });
      return;
    }
    if (item.appointmentBooked && !item.appointmentMine) {
      const detail =
        this.resolveOccupancyDetail(item) || "与当前所选时段重叠";
      wx.showModal({
        title: "该时段已被预约",
        content:
          (seatLabel ? "座位 " + seatLabel + "\n占用时段：\n" : "占用时段：\n") +
          detail,
        showCancel: false,
      });
      return;
    }
    if (item.ruleBlocked) {
      const detail = this.resolveOccupancyDetail(item);
      wx.showModal({
        title: "该时段不可预约",
        content:
          (seatLabel ? "座位 " + seatLabel + "\n" : "") +
          (detail ? "限制时段：\n" + detail : "后台时段策略限制，请换座或改时段"),
        showCancel: false,
      });
      return;
    }
    if (item.locked == 1 || item.roomState == "1") {
      var lockDetail = this.resolveOccupancyDetail(item);
      wx.showModal({
        title: "座位不可用",
        content:
          (seatLabel ? "座位 " + seatLabel + "\n" : "") +
          (lockDetail ? "限制时段：\n" + lockDetail : "后台已将该座位设为维修/禁用。"),
        showCancel: false,
      });
      return;
    }
    this.setData({
      seatIndex: idx,
      "dataFrom.seatId": String(item.seatId != null ? item.seatId : item.id),
    });
  },

  /**
   * 合并座位占用覆盖层（正式预约等）；返回 Promise，便于与加载态配合。
   */
  refreshBrowseOverlay() {
    const roomId = this.data.dataFrom.roomId;
    const biz = (this.data.stateTime || "").substr(0, 10);
    const raw = this.data.seatList || [];
    const applyOverlay = (ov) => {
      const merged = this.mergeBrowseOverlay(raw, Array.isArray(ov) ? ov : []);
      const coord = this.buildCoordSeatMap(merged);
      const mega =
        merged.length && !coord ? this.buildMegaFloorMap(merged) : null;
      return new Promise((resolve) => {
        this.setData(
          {
            seatList: merged,
            seatCoordMap: coord,
            seatMegaMap: mega,
            browseOverlayMap: Array.isArray(ov) ? ov : [],
          },
          resolve
        );
      });
    };
    if (!roomId || biz.length !== 10 || !raw.length) {
      return applyOverlay([]);
    }
    const seatDayFull =
      biz.length === 10 && this.data.slotStart && this.data.slotEnd
        ? biz + "/" + this.data.slotStart + "-" + this.data.slotEnd
        : "";
    return browseOverlay(roomId, biz, seatDayFull)
      .then((info) => {
        const ov = info.code === 0 && Array.isArray(info.data) ? info.data : [];
        return applyOverlay(ov);
      })
      .catch(() => applyOverlay([]));
  },

  // 获取座位
  getSeat() {
    const rid = this.data.dataFrom.roomId;
    if (!rid) {
      this.setData({
        seatList: [],
        seatCoordMap: null,
        seatMegaMap: null,
        seatLoading: false,
      });
      return;
    }
    this.setData({ seatLoading: true });
    getSeatList(rid)
      .then((info) => {
        if (info.code == 0) {
          const raw = Array.isArray(info.data) ? info.data : [];
          this.setData(
            {
              seatList: raw,
              seatCoordMap: null,
              seatMegaMap: null,
              seatIndex: -1,
              "dataFrom.seatId": "",
            },
            () => {
              Promise.resolve(this.refreshBrowseOverlay()).finally(() => {
                this.setData({ seatLoading: false });
              });
            }
          );
        } else {
          this.setData({ seatLoading: false });
          if (info.code === 401) {
            wx.showToast({ title: info.msg || "请先登录", icon: "none" });
          } else {
            wx.showToast({ title: info.msg || "座位加载失败", icon: "none" });
          }
        }
      })
      .catch((e) => {
        this.setData({ seatLoading: false });
        wx.showToast({
          title: formatHttpError(e, "网络异常"),
          icon: "none",
          duration: 3200,
        });
      });
  },
  // 预定
  regionCheck() {
    const d1 = this.data.stateTime.substr(0, 10);
    const d2 = this.data.endTime.substr(0, 10);
    if (d1 !== d2) {
      wx.showToast({
        title: "开始与结束须为同一天",
        icon: "none",
        duration: 2000,
      });
      return;
    }
    const pickDay = this.parseLocalYmd(d1);
    if (
      Number.isNaN(pickDay) ||
      pickDay < this.startOfDayMs(this.data.minDate) ||
      pickDay > this.startOfDayMs(this.data.maxDate)
    ) {
      wx.showToast({
        title:
          "预约日期仅可选今日起 " + this.data.advanceBookingDays + " 天内",
        icon: "none",
        duration: 2500,
      });
      return;
    }
    const sm = this.timeToMinutes(this.data.slotStart);
    const em = this.timeToMinutes(this.data.slotEnd);
    if (em <= sm || em - sm < MIN_BOOKING_DURATION_MINUTES) {
      wx.showToast({
        title:
          "结束须晚于开始，且单次至少 " + MIN_BOOKING_DURATION_MINUTES / 60 + " 小时",
        icon: "none",
        duration: 2400,
      });
      return;
    }

    const seatDay = d1 + "/" + this.data.slotStart + "-" + this.data.slotEnd;
    this.setData({
      "dataFrom.seatDay": seatDay,
    });

    const pickDay0 = this.startOfDayMs(this.parseLocalYmd(d1));
    const today0 = this.startOfDayMs(Date.now());
    if (pickDay0 === today0) {
      const chosenEnd = this.combineDaySlotMs(pickDay0, this.data.slotEnd);
      if (chosenEnd <= Date.now()) {
        wx.showToast({
          title: "当日所选时段已结束",
          icon: "none",
          duration: 2600,
        });
        return;
      }
      const earliest = this.nextSlotStartAfterNow(pickDay0, Date.now());
      const chosenStart = this.combineDaySlotMs(pickDay0, this.data.slotStart);
      if (earliest == null || chosenStart < earliest) {
        wx.showToast({
          title: "当日开始须不早于当前仍可选的最早档",
          icon: "none",
          duration: 2800,
        });
        return;
      }
    }

    if (!wx.getStorageSync("token")) {
      wx.showToast({ title: "请先登录后再预约", icon: "none", duration: 2000 });
      return;
    }
    if (this.data.dataFrom.seatId === "" || this.data.dataFrom.seatId == null) {
      wx.showToast({
        title: "请先选择座位",
        icon: "none",
        duration: 2000,
      });
      return;
    }
    const sid = this.data.dataFrom.seatId;
    const payload = Object.assign({}, this.data.dataFrom, {
      seatDay,
      seatId: sid != null && sid !== "" ? Number(sid) : sid,
    });

    wx.showLoading({ title: "校验中", mask: true });
    quoteAppointment(payload)
      .then((q) => {
        wx.hideLoading();
        if (q.code !== 0) {
          this.showBookFail((q && q.msg) || "询价失败，请稍后重试");
          return;
        }
        const row = q.data || {};
        const amount =
          row.amountYuan != null ? Number(row.amountYuan) : 0;
        try {
          wx.setStorageSync("__pending_appt", JSON.stringify(payload));
          wx.setStorageSync("__pending_appt_quote", JSON.stringify(row));
        } catch (e) {}
        wx.navigateTo({
          url: `${pages.MockPay}?scene=appt&amount=${encodeURIComponent(
            String(amount)
          )}`,
        });
      })
      .catch((e) => {
        wx.hideLoading();
        wx.showToast({
          title: formatHttpError(e, "网络异常"),
          icon: "none",
          duration: 3200,
        });
      });
  },
  /**
   * 生命周期函数--监听页面初次渲染完成
   */
  onReady() {},

  /**
   * 生命周期函数--监听页面显示
   */
  onShow() {
    if (this.data.dataFrom.roomId && (this.data.seatList || []).length) {
      this.refreshBrowseOverlay();
    }
  },

  /**
   * 生命周期函数--监听页面隐藏
   */
  onHide() {},

  /**
   * 生命周期函数--监听页面卸载
   */
  onUnload() {},

  /**
   * 页面相关事件处理函数--监听用户下拉动作
   */
  onPullDownRefresh() {},

  /**
   * 页面上拉触底事件的处理函数
   */
  onReachBottom() {},

  /**
   * 用户点击右上角分享
   */
  onShareAppMessage() {},
});
