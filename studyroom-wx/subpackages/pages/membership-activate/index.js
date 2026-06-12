const pages = require("../../../pages/index.js");
const { activateMembership, mineMembership } = require("../../../api/membership");
const {
  todayStr,
  clampYmd,
  fromApiDate,
  maxActivateDateStr,
  DEFAULT_MAX_ACTIVATE_YEARS,
} = require("../../../utils/membershipDate");

Page({
  data: {
    orderId: "",
    cardName: "",
    validityDays: 30,
    mode: "new",
    changesLeft: 3,
    activateDate: todayStr(),
    minDate: todayStr(),
    maxDate: maxActivateDateStr(DEFAULT_MAX_ACTIVATE_YEARS),
    maxActivateYears: DEFAULT_MAX_ACTIVATE_YEARS,
    submitting: false,
  },

  onLoad(query) {
    const orderId = query.orderId || "";
    const cardName = query.cardName ? decodeURIComponent(query.cardName) : "";
    const validityDays = query.validityDays ? Number(query.validityDays) : 30;
    const mode = query.mode === "modify" ? "modify" : "new";
    const initDate = query.activateDate || todayStr();
    const changesLeft =
      query.changesLeft != null && query.changesLeft !== ""
        ? Number(query.changesLeft)
        : 3;
    const today = todayStr();
    const maxDate = maxActivateDateStr(DEFAULT_MAX_ACTIVATE_YEARS);
    wx.setNavigationBarTitle({
      title: mode === "modify" ? "修改生效日" : "激活会员卡",
    });
    this.setData({
      orderId,
      cardName,
      validityDays: validityDays > 0 ? validityDays : 30,
      mode,
      changesLeft: changesLeft >= 0 ? changesLeft : 0,
      activateDate: clampYmd(initDate, today, maxDate),
      minDate: today,
      maxDate,
    });
    if (!orderId) {
      wx.showToast({ title: "缺少订单", icon: "none" });
      return;
    }
    this.loadOrderMeta(orderId);
  },

  loadOrderMeta(orderId) {
    mineMembership()
      .then((info) => {
        if (info.code !== 0 || !info.data) return;
        const d = info.data;
        const today = todayStr();
        let maxDate = maxActivateDateStr(DEFAULT_MAX_ACTIVATE_YEARS);
        if (d.maxActivateDate) {
          const apiMax = fromApiDate(d.maxActivateDate);
          if (apiMax) maxDate = apiMax;
        }
        const patch = { minDate: today, maxDate };
        if (d.minActivateDate) {
          const apiMin = fromApiDate(d.minActivateDate);
          if (apiMin) patch.minDate = apiMin;
        }
        const pending = Array.isArray(d.pendingActivation) ? d.pendingActivation : [];
        const orders = Array.isArray(d.orders) ? d.orders : [];
        const row =
          pending.find((p) => String(p.id) === String(orderId)) ||
          orders.find((p) => String(p.id) === String(orderId));
        if (!row) {
          this.setData(patch);
          return;
        }
        patch.cardName = row.cardName || "";
        patch.validityDays =
          row.validityDays != null && row.validityDays > 0 ? row.validityDays : 30;
        if (this.data.mode === "modify") {
          const left =
            row.activateChangesLeft != null
              ? Number(row.activateChangesLeft)
              : this.data.changesLeft;
          patch.changesLeft = left >= 0 ? left : 0;
          if (row.validFrom) {
            patch.activateDate = clampYmd(
              String(row.validFrom).replace("T", " ").slice(0, 10),
              patch.minDate,
              patch.maxDate
            );
          }
          if (!row.canModifyActivate) {
            wx.showModal({
              title: "无法修改",
              content:
                left <= 0
                  ? "该卡激活日期已修改 3 次，无法继续修改"
                  : "会员已生效或已过期，无法修改",
              showCancel: false,
              success: () => wx.navigateBack(),
            });
          }
        } else {
          patch.activateDate = clampYmd(this.data.activateDate, patch.minDate, patch.maxDate);
        }
        this.setData(patch);
      })
      .catch(() => {});
  },

  onDateChange(e) {
    const { minDate, maxDate } = this.data;
    const picked = clampYmd(e.detail.value, minDate, maxDate);
    this.setData({ activateDate: picked });
    if (picked !== e.detail.value) {
      wx.showToast({ title: "生效日须在两年内", icon: "none" });
    }
  },

  onConfirm() {
    const { orderId, activateDate, submitting, mode, changesLeft, minDate, maxDate } =
      this.data;
    if (submitting) return;
    if (!orderId) {
      wx.showToast({ title: "缺少订单", icon: "none" });
      return;
    }
    const date = clampYmd(activateDate, minDate, maxDate);
    if (!date) {
      wx.showToast({ title: "请选择生效日期", icon: "none" });
      return;
    }
    if (date !== activateDate) {
      wx.showToast({ title: "生效日须在两年内", icon: "none" });
      this.setData({ activateDate: date });
      return;
    }
    if (mode === "modify" && changesLeft <= 0) {
      wx.showToast({ title: "修改次数已用完", icon: "none" });
      return;
    }
    this.setData({ submitting: true });
    activateMembership(Number(orderId), date)
      .then((info) => {
        this.setData({ submitting: false });
        if (info.code === 0) {
          wx.showToast({
            title: mode === "modify" ? "修改成功" : "激活成功",
            icon: "success",
          });
          setTimeout(() => {
            wx.redirectTo({ url: pages.MembershipMine });
          }, 400);
        } else {
          wx.showToast({ title: info.msg || "激活失败", icon: "none" });
        }
      })
      .catch(() => {
        this.setData({ submitting: false });
        wx.showToast({ title: "网络异常", icon: "none" });
      });
  },

  onLater() {
    wx.redirectTo({ url: pages.MembershipMine });
  },
});
