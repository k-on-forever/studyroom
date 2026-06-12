const pages = require("../index.js");
const { login } = require("../../api/login");
const { formatHttpError } = require("../../utils/http");

Page({
  data: {
    account: "",
    password: "",
    loggedIn: false,
    showPassword: false,
  },

  toggleShowPassword() {
    this.setData({ showPassword: !this.data.showPassword });
  },

  onShow() {
    this.setData({ loggedIn: !!wx.getStorageSync("token") });
  },

  onField(e) {
    const field = e.currentTarget.dataset.field;
    if (!field) return;
    this.setData({ [field]: e.detail.value });
  },

  goRegister() {
    wx.navigateTo({ url: pages.Register });
  },

  goForgot() {
    wx.navigateTo({ url: pages.ForgotPassword });
  },

  onLogin() {
    const { account, password } = this.data;
    if (!account || !password) {
      wx.showToast({ title: "请填写帐号和密码", icon: "none" });
      return;
    }
    login({ account: account.trim(), password })
      .then((info) => {
        const token = info.token || (info.data && info.data.token);
        if (info.code === 0 && token) {
          wx.setStorageSync("token", token);
          this.setData({ loggedIn: true });
          wx.showToast({ title: "登录成功", icon: "success" });
          setTimeout(() => {
            wx.switchTab({ url: pages.Home });
          }, 400);
        } else {
          wx.showToast({ title: info.msg || "登录失败", icon: "none" });
        }
      })
      .catch((e) => {
        wx.showToast({
          title: formatHttpError(e, "网络错误"),
          icon: "none",
          duration: 3200,
        });
      });
  },

  onLogout() {
    wx.removeStorageSync("token");
    this.setData({ loggedIn: false, password: "", showPassword: false });
    wx.showToast({ title: "已退出登录", icon: "none" });
  },

  onBack() {
    wx.navigateBack({ fail: () => wx.switchTab({ url: pages.Home }) });
  },
});
