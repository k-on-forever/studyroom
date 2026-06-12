const pages = require("../../../pages/index.js");
const { mineMembership } = require("../../../api/membership");

function fmtDate(d) {
  if (!d) return "";
  if (typeof d === "string") return d.replace("T", " ").slice(0, 19);
  return String(d);
}

Page({
  data: {
    loaded: false,
    activeText: "加载中…",
    validToText: "",
    validFromText: "",
    maxActivateChanges: 3,
    pending: [],
    orders: [],
  },

  onShow() {
    const token = wx.getStorageSync("token");
    if (!token) {
      this.setData({
        loaded: true,
        activeText: "未登录",
        validToText: "",
        validFromText: "",
        pending: [],
        orders: [],
      });
      return;
    }
    mineMembership()
      .then((info) => {
        if (info.code === 0 && info.data) {
          const d = info.data;
          const active = !!d.active;
          const pending = Array.isArray(d.pendingActivation)
            ? d.pendingActivation
            : [];
          const pendingDays = {};
          pending.forEach((p) => {
            pendingDays[p.id] = p.validityDays;
          });
          const maxChg =
            d.maxActivateChanges != null ? Number(d.maxActivateChanges) : 3;
          const orders = enrichOrders(
            Array.isArray(d.orders) ? d.orders : [],
            pendingDays
          );
          const activeOrder = orders.find(
            (o) => o.payStatus === 1 && o.validFrom && o.validTo && o._activeNow
          );
          this.setData({
            loaded: true,
            activeText: active
              ? "会员有效（预约 0 元）"
              : pending.length
                ? "有待激活的会员卡"
                : "非会员 / 已过期",
            validFromText: activeOrder ? fmtDate(activeOrder.validFrom) : "",
            validToText: d.validTo ? fmtDate(d.validTo) : "",
            maxActivateChanges: maxChg,
            pending,
            orders,
          });
        } else if (info.code === 401) {
          this.setData({
            loaded: true,
            activeText: "请先登录",
            validToText: "",
            validFromText: "",
            pending: [],
            orders: [],
          });
        } else {
          this.setData({
            loaded: true,
            activeText: "加载失败",
            pending: [],
            orders: [],
          });
        }
      })
      .catch(() => {
        this.setData({
          loaded: true,
          activeText: "网络异常",
          pending: [],
          orders: [],
        });
      });
  },

  goActivate(e) {
    const id = e.currentTarget.dataset.id;
    const name = e.currentTarget.dataset.name || "";
    const days = e.currentTarget.dataset.days || 30;
    const mode = e.currentTarget.dataset.mode || "new";
    const date = e.currentTarget.dataset.date || "";
    const left = e.currentTarget.dataset.left;
    if (!id) return;
    let url = `${pages.MembershipActivate}?orderId=${id}&cardName=${encodeURIComponent(name)}&validityDays=${days}&mode=${mode}`;
    if (date) url += `&activateDate=${date}`;
    if (left != null && left !== "") url += `&changesLeft=${left}`;
    wx.navigateTo({ url });
  },
});

function enrichOrders(orders, pendingDays) {
  const now = Date.now();
  const daysMap = pendingDays || {};
  return orders.map((o) => {
    const from = o.validFrom ? new Date(String(o.validFrom).replace(/-/g, "/")).getTime() : 0;
    const to = o.validTo ? new Date(String(o.validTo).replace(/-/g, "/")).getTime() : 0;
    const paid = o.payStatus === 1;
    const pendingAct = paid && !o.validFrom;
    const activeNow = paid && from && to && from <= now && to > now;
    const scheduled = paid && from && from > now;
    const usedChg = o.activateChangeCount != null ? Number(o.activateChangeCount) : 0;
    const chgLeft =
      o.activateChangesLeft != null
        ? Number(o.activateChangesLeft)
        : Math.max(0, 3 - usedChg);
    const canModify = !!o.canModifyActivate;
    let statusTag = "待付";
    if (pendingAct) statusTag = "待激活";
    else if (scheduled) statusTag = "待生效";
    else if (paid && activeNow) statusTag = "生效中";
    else if (paid && o.validTo) statusTag = "已过期";
    else if (paid) statusTag = "已付";
    const validFromDate = o.validFrom
      ? String(o.validFrom).replace("T", " ").slice(0, 10)
      : "";
    return Object.assign({}, o, {
      _pendingAct: pendingAct,
      _activeNow: activeNow,
      _scheduled: scheduled,
      _canModify: canModify,
      statusTag,
      validityDays: daysMap[o.id] || 30,
      activateChangeCount: usedChg,
      activateChangesLeft: chgLeft,
      validFromDate,
    });
  });
}
