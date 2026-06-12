const pages = require("../index.js");
const { mineMembership } = require("../../api/membership");
const { userInfo } = require("../../api/information");

/** 个人中心全部入口（圆格），与底部列表去重合并 */
function buildMenuList(hasToken) {
  const list = [
    {
      key: "seat",
      label: "入座签到",
      icon: "入",
      theme: "sky",
      handler: "goApplication",
    },
    {
      key: "record",
      label: "订座记录",
      icon: "座",
      theme: "pink",
      handler: "goBookingRecord",
    },
    {
      key: "stats",
      label: "学习统计",
      icon: "统",
      theme: "teal",
      handler: "goStudyStats",
    },
    {
      key: "buy",
      label: "购买会员卡",
      icon: "卡",
      theme: "mint",
      handler: "goMembershipBuy",
    },
    {
      key: "activate",
      label: "会员卡激活",
      icon: "激",
      theme: "gold",
      handler: "goMembershipActivate",
    },
    {
      key: "member",
      label: "我的会员",
      icon: "员",
      theme: "sand",
      handler: "goMembershipMine",
    },
    {
      key: "profile",
      label: "个人信息",
      icon: "我",
      theme: "lavender",
      handler: "goInformation",
    },
    {
      key: "feedback",
      label: "留言反馈",
      icon: "言",
      theme: "coral",
      handler: "goMessage",
    },
    {
      key: "account",
      label: hasToken ? "切换帐号" : "帐号登录",
      icon: "帐",
      theme: "blue",
      handler: "goLogin",
    },
  ];
  if (!hasToken) {
    list.push({
      key: "register",
      label: "注册帐号",
      icon: "注",
      theme: "green",
      handler: "goRegister",
    });
  } else {
    list.push({
      key: "logout",
      label: "退出登录",
      icon: "退",
      theme: "gray",
      handler: "onLogout",
    });
  }
  return list;
}

Page({
  data: {
    hasToken: false,
    menuList: [],
    userInfo: { name: '', mobile: '', userImg: '' },
  },

  onLoad() {
    this.syncTabBar();
    this.refreshMenuList();
  },

  onShow() {
    const hasToken = !!wx.getStorageSync("token");
    this.setData({ hasToken });
    if (hasToken) this.loadUserInfo();
    this.refreshMenuList();
    this.syncTabBar();
  },

  loadUserInfo() {
    userInfo().then((info) => {
      if (info.code == 0 && info.data) {
        this.setData({ userInfo: info.data });
      }
    }).catch((e) => {
      if (e && (e.statusCode === 401 || (e.data && e.data.code === 401))) {
        wx.removeStorageSync("token");
        this.refreshMenuList();
      }
    });
  },

  refreshMenuList() {
    const hasToken = !!wx.getStorageSync("token");
    this.setData({
      hasToken,
      menuList: buildMenuList(hasToken),
    });
  },

  syncTabBar() {
    if (typeof this.getTabBar !== "function") return;
    const bar = this.getTabBar();
    if (bar && typeof bar.setData === "function") {
      bar.setData({ selected: 2 });
    }
  },

  onMenuTap(e) {
    const handler = e.currentTarget.dataset.handler;
    if (!handler || typeof this[handler] !== "function") return;
    this[handler]();
  },

  goApplication() {
    wx.switchTab({ url: pages.Application });
  },

  goStudyStats() {
    wx.navigateTo({ url: pages.StudyStats });
  },

  goLogin() {
    wx.navigateTo({ url: pages.Login });
  },

  onLogout() {
    wx.removeStorageSync("token");
    this.refreshMenuList();
    wx.showToast({ title: "已退出登录", icon: "none" });
  },

  goRegister() {
    wx.navigateTo({ url: pages.Register });
  },

  goInformation() {
    wx.navigateTo({ url: pages.Information });
  },

  goMessage() {
    wx.navigateTo({ url: pages.Message });
  },

  goMembershipBuy() {
    wx.navigateTo({ url: pages.MembershipBuy });
  },

  goMembershipMine() {
    wx.navigateTo({ url: pages.MembershipMine });
  },

  goMembershipActivate() {
    if (!wx.getStorageSync("token")) {
      wx.showModal({
        title: "提示",
        content: "请先登录后再激活会员卡",
        confirmText: "去登录",
        success: (r) => {
          if (r.confirm) wx.navigateTo({ url: pages.Login });
        },
      });
      return;
    }
    wx.showLoading({ title: "加载中" });
    mineMembership()
      .then((info) => {
        wx.hideLoading();
        if (info.code === 401) {
          wx.showToast({ title: "请先登录", icon: "none" });
          return;
        }
        if (info.code !== 0 || !info.data) {
          wx.showToast({ title: info.msg || "加载失败", icon: "none" });
          return;
        }
        const pending = Array.isArray(info.data.pendingActivation)
          ? info.data.pendingActivation
          : [];
        if (pending.length === 0) {
          wx.showModal({
            title: "暂无待激活会员卡",
            content: "购买会员卡并完成支付后，可在此选择生效日激活权益。",
            confirmText: "去购买",
            cancelText: "知道了",
            success: (r) => {
              if (r.confirm) wx.navigateTo({ url: pages.MembershipBuy });
            },
          });
          return;
        }
        if (pending.length > 1) {
          wx.navigateTo({ url: pages.MembershipMine });
          return;
        }
        const p = pending[0];
        const days = p.validityDays || 30;
        const name = encodeURIComponent(p.cardName || "");
        wx.navigateTo({
          url: `${pages.MembershipActivate}?orderId=${p.id}&cardName=${name}&validityDays=${days}&mode=new`,
        });
      })
      .catch(() => {
        wx.hideLoading();
        wx.showToast({ title: "网络异常", icon: "none" });
      });
  },

  goBookingRecord() {
    if (!wx.getStorageSync("token")) {
      wx.showToast({ title: "请先登录", icon: "none" });
      return;
    }
    wx.navigateTo({ url: pages.BookingRecord });
  },

});
