const pages = require("../../../pages/index.js");
const { listCards, createMembershipOrder } = require("../../../api/membership");

Page({
  data: {
    cards: [],
    loaded: false,
  },

  onLoad() {
    listCards()
      .then((info) => {
        if (info.code === 0) {
          const list = Array.isArray(info.data) ? info.data : [];
          this.setData({ cards: list, loaded: true });
        } else {
          this.setData({ loaded: true });
          wx.showToast({ title: info.msg || "加载失败", icon: "none" });
        }
      })
      .catch((e) => {
        this.setData({ loaded: true });
        const msg = (e && e.errMsg) || "网络异常：检查 baseUrl 与后端是否启动";
        wx.showToast({ title: msg.length > 36 ? msg.slice(0, 36) + "…" : msg, icon: "none" });
      });
  },

  onBuy(e) {
    const token = wx.getStorageSync("token");
    if (!token) {
      wx.showModal({
        title: "提示",
        content: "请先登录再购买会员卡",
        confirmText: "去登录",
        success: (r) => {
          if (r.confirm) wx.navigateTo({ url: pages.Login });
        },
      });
      return;
    }
    const id = e.currentTarget.dataset.id;
    const price = e.currentTarget.dataset.price;
    wx.showLoading({ title: "创建订单" });
    createMembershipOrder(id)
      .then((info) => {
        wx.hideLoading();
        if (info.code === 0 && info.orderId != null) {
          wx.navigateTo({
            url: `${pages.MockPay}?scene=mship&orderId=${info.orderId}&amount=${price || 0}`,
          });
        } else if (info.code === 401) {
          wx.showModal({
            title: "提示",
            content: info.msg || "请先登录",
            confirmText: "去登录",
            success: (r) => {
              if (r.confirm) wx.navigateTo({ url: pages.Login });
            },
          });
        } else {
          wx.showToast({ title: info.msg || "下单失败", icon: "none" });
        }
      })
      .catch(() => {
        wx.hideLoading();
        wx.showToast({ title: "网络异常", icon: "none" });
      });
  },
});
