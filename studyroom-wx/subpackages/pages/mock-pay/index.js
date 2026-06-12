const pages = require("../../../pages/index.js");
const { mockPayMembership } = require("../../../api/membership");
const { addSeat } = require("../../../api/region");
const { quoteAppointment } = require("../../../api/application");
const {
  getPayConfig,
  prepayAppointment,
  prepayMembership,
  mockConfirmPay,
  afterPay,
} = require("../../../api/pay");
const { formatSeatDayForDisplay } = require("../../../utils/appointmentFormat");

Page({
  data: {
    scene: "",
    orderId: "",
    displayAmount: "0.00",
    quoteLoading: false,
    quote: null,
    canCashPay: false,
    orderSummary: null,
    payMode: "mock",
    mockPayAllowed: true,
  },

  onLoad(query) {
    const scene = query.scene || "appt";
    const amount = query.amount != null ? String(query.amount) : "0";
    const orderId = query.orderId || "";
    this.setData({
      scene,
      orderId,
      displayAmount: formatMoney(amount),
    });
    getPayConfig()
      .then((info) => {
        if (info.code === 0 && info.data) {
          this.setData({
            payMode: info.data.payMode || "mock",
            mockPayAllowed: !!info.data.mockPayAllowed,
          });
        }
      })
      .catch(() => {});
    if (scene === "appt") {
      this.loadOrderSummary();
      this.refreshQuote();
    }
  },

  loadOrderSummary() {
    const body = this.parsePendingBody();
    if (!body) {
      this.setData({ orderSummary: null });
      return;
    }
    const seatDay = body.seatDay || "";
    const seatLabel =
      body.seatName || (body.seatId != null ? "座位 " + body.seatId : "—");
    this.setData({
      orderSummary: {
        seatLabel,
        seatDayDisplay: formatSeatDayForDisplay(seatDay) || "—",
      },
    });
  },

  refreshQuote() {
    const cached = wx.getStorageSync("__pending_appt_quote");
    if (cached) {
      try {
        const row = JSON.parse(cached);
        this.applyQuote(row);
        return;
      } catch (e) {
        /* fall through */
      }
    }
    const raw = wx.getStorageSync("__pending_appt");
    if (!raw) {
      this.setData({ quoteLoading: false, quote: null, canCashPay: false });
      return;
    }
    let body;
    try {
      body = JSON.parse(raw);
    } catch (e) {
      this.setData({ quoteLoading: false, quote: null, canCashPay: false });
      return;
    }
    this.setData({ quoteLoading: true });
    quoteAppointment(body)
      .then((q) => {
        if (q.code !== 0) {
          this.setData({ quoteLoading: false, quote: null, canCashPay: false });
          wx.showModal({
            title: "无法询价",
            content: (q && q.msg) || "座位或时段已变化，请返回选座重试",
            showCancel: false,
          });
          return;
        }
        const row = q.data || {};
        try {
          wx.setStorageSync("__pending_appt_quote", JSON.stringify(row));
        } catch (e) {}
        this.applyQuote(row);
      })
      .catch(() => {
        this.setData({ quoteLoading: false, quote: null, canCashPay: false });
        wx.showToast({ title: "询价失败，请检查网络", icon: "none" });
      });
  },

  applyQuote(row) {
    const amt = row.amountYuan != null ? Number(row.amountYuan) : 0;
    const canCashPay = !row.memberFree && amt > 0;
    this.setData({
      quoteLoading: false,
      quote: row,
      displayAmount: formatMoney(amt),
      canCashPay,
    });
  },

  parsePendingBody() {
    const raw = wx.getStorageSync("__pending_appt");
    if (!raw) return null;
    try {
      return JSON.parse(raw);
    } catch (e) {
      return null;
    }
  },

  ensureFreshQuoteThen(run) {
    const body = this.parsePendingBody();
    if (!body) {
      wx.showToast({ title: "无待支付预约", icon: "none" });
      return;
    }
    wx.showLoading({ title: "校验座位", mask: true });
    quoteAppointment(body)
      .then((q) => {
        wx.hideLoading();
        if (q.code !== 0) {
          wx.showModal({
            title: "无法预约",
            content: (q && q.msg) || "座位或时段已变化，请返回修改",
            showCancel: false,
          });
          return;
        }
        const row = q.data || {};
        try {
          wx.setStorageSync("__pending_appt_quote", JSON.stringify(row));
        } catch (e) {}
        this.applyQuote(row);
        run(body, row);
      })
      .catch(() => {
        wx.hideLoading();
        wx.showToast({ title: "网络异常", icon: "none" });
      });
  },

  submitAppointment(body, opts) {
    wx.showLoading({ title: opts.loadingTitle || "提交中", mask: true });
    addSeat(body)
      .then((info) => {
        wx.hideLoading();
        if (info.code === 0) {
          wx.removeStorageSync("__pending_appt");
          wx.removeStorageSync("__pending_appt_quote");
          wx.showToast({ title: "预约成功", icon: "success" });
          setTimeout(() => wx.switchTab({ url: pages.Application }), 400);
        } else if (info.code === 40210) {
          wx.showToast({
            title: info.msg || "仍需完成支付",
            icon: "none",
            duration: 2600,
          });
          this.refreshQuote();
        } else {
          wx.showToast({ title: info.msg || "预约失败", icon: "none" });
        }
      })
      .catch(() => {
        wx.hideLoading();
        wx.showToast({ title: "网络异常", icon: "none" });
      });
  },

  runCashPay(body, q) {
    const amt = q.amountYuan != null ? Number(q.amountYuan) : 0;
    if (q.memberFree || amt <= 0) {
      wx.showToast({ title: "本单无需微信支付", icon: "none" });
      return;
    }
    wx.showLoading({ title: "拉起支付", mask: true });
    const prepay =
      this.data.scene === "mship"
        ? prepayMembership(Number(this.data.orderId))
        : prepayAppointment(body);
    prepay
      .then((info) => {
        wx.hideLoading();
        if (info.code !== 0 || !info.data) {
          wx.showToast({ title: (info && info.msg) || "下单失败", icon: "none" });
          return;
        }
        const d = info.data;
        if (d.needPay === false) {
          this.submitAppointment(body, { loadingTitle: "提交预约" });
          return;
        }
        const outTradeNo = d.outTradeNo;
        if (d.mock && this.data.mockPayAllowed) {
          wx.showLoading({ title: "支付中", mask: true });
          mockConfirmPay(outTradeNo)
            .then((c) => {
              wx.hideLoading();
              if (c.code !== 0) {
                wx.showToast({ title: c.msg || "失败", icon: "none" });
                return;
              }
              if (this.data.scene === "mship") {
                this.afterMembershipPaid(outTradeNo);
                return;
              }
              const next = Object.assign({}, body, { wxPayOutTradeNo: outTradeNo });
              delete next.simulatePaidNonMember;
              delete next.simulatedPayChannel;
              this.submitAppointment(next, { loadingTitle: "提交预约" });
            })
            .catch(() => {
              wx.hideLoading();
              wx.showToast({ title: "网络异常", icon: "none" });
            });
          return;
        }
        if (!d.timeStamp) {
          wx.showModal({
            title: "微信支付未就绪",
            content: "请配置 study.wx.pay.mode=wx 及商户证书，或使用微信登录获取 openid。",
            showCancel: false,
          });
          return;
        }
        wx.requestPayment({
          timeStamp: d.timeStamp,
          nonceStr: d.nonceStr,
          package: d.package,
          signType: d.signType || "RSA",
          paySign: d.paySign,
          success: () => {
            if (this.data.scene === "mship") {
              this.afterMembershipPaid(outTradeNo);
              return;
            }
            const next = Object.assign({}, body, { wxPayOutTradeNo: outTradeNo });
            delete next.simulatePaidNonMember;
            this.submitAppointment(next, { loadingTitle: "提交预约" });
          },
          fail: (err) => {
            if (err && err.errMsg && err.errMsg.indexOf("cancel") >= 0) {
              wx.showToast({ title: "已取消支付", icon: "none" });
            } else {
              wx.showToast({ title: "支付失败", icon: "none" });
            }
          },
        });
      })
      .catch(() => {
        wx.hideLoading();
        wx.showToast({ title: "网络异常", icon: "none" });
      });
  },

  afterMembershipPaid(outTradeNo) {
    const done = () => {
      wx.showToast({ title: "支付成功", icon: "success" });
      setTimeout(() => {
        wx.redirectTo({
          url: `${pages.MembershipActivate}?orderId=${this.data.orderId}`,
        });
      }, 400);
    };
    if (outTradeNo) {
      afterPay(outTradeNo).finally(done);
    } else {
      done();
    }
  },

  onPayWithMembership() {
    this.ensureFreshQuoteThen((body, q) => {
      if (!q.memberFree) {
        wx.showModal({
          title: "会员卡不可用",
          content: "当前账号无有效会员卡。是否前往购买？",
          confirmText: "去购买",
          cancelText: "取消",
          success: (res) => {
            if (res.confirm) {
              wx.navigateTo({ url: pages.MembershipBuy });
            }
          },
        });
        return;
      }
      const next = Object.assign({}, body);
      delete next.simulatePaidNonMember;
      delete next.simulatedPayChannel;
      delete next.wxPayOutTradeNo;
      this.submitAppointment(next, { loadingTitle: "会员预约" });
    });
  },

  onSimulateWechat() {
    this.ensureFreshQuoteThen((body, q) => {
      this.runCashPay(body, q);
    });
  },

  onSubmitZero() {
    this.ensureFreshQuoteThen((body, q) => {
      if (!q.memberFree) {
        wx.showToast({ title: "请购买会员卡或选择微信支付", icon: "none" });
        return;
      }
      const next = Object.assign({}, body);
      delete next.simulatePaidNonMember;
      delete next.simulatedPayChannel;
      delete next.wxPayOutTradeNo;
      this.submitAppointment(next, { loadingTitle: "提交预约" });
    });
  },

  onConfirmPay() {
    const { scene, orderId } = this.data;
    if (scene === "mship") {
      if (!orderId) {
        wx.showToast({ title: "缺少订单", icon: "none" });
        return;
      }
      if (this.data.mockPayAllowed) {
        wx.showLoading({ title: "处理中" });
        mockPayMembership(Number(orderId))
          .then((info) => {
            wx.hideLoading();
            if (info.code === 0) {
              this.afterMembershipPaid();
            } else {
              wx.showToast({ title: info.msg || "失败", icon: "none" });
            }
          })
          .catch(() => {
            wx.hideLoading();
            wx.showToast({ title: "网络异常", icon: "none" });
          });
        return;
      }
      this.runCashPay({}, { amountYuan: Number(this.data.displayAmount) });
      return;
    }
    wx.showToast({ title: "请选择支付方式", icon: "none" });
  },

  onCancel() {
    wx.navigateBack({ fail: () => wx.switchTab({ url: pages.Home }) });
  },
});

function formatMoney(v) {
  const n = Number(v);
  if (Number.isNaN(n)) return "0.00";
  return n.toFixed(2);
}
