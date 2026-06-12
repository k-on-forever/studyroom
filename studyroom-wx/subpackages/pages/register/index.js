const pages = require("../../../pages/index.js");
const { register, sendRegisterSms } = require("../../../api/register");
const { login } = require("../../../api/login");

Page({
  data: {
    dataRegisterFrom: {
      account: "",
      password: "",
      confirmPassword: "",
      smsCode: "",
    },
    smsSeconds: 0,
    smsBtnText: "获取验证码",
    showPassword: false,
    showConfirmPassword: false,
  },

  pickVal(e) {
    const d = e.detail;
    if (d == null) return "";
    if (typeof d === "string" || typeof d === "number") return String(d);
    if (typeof d === "object" && d.value != null) return String(d.value);
    return "";
  },

  onAccount(e) {
    this.setData({ "dataRegisterFrom.account": this.pickVal(e) });
  },

  onPassword(e) {
    this.setData({ "dataRegisterFrom.password": this.pickVal(e) });
  },

  onConfirmPassword(e) {
    this.setData({ "dataRegisterFrom.confirmPassword": this.pickVal(e) });
  },

  toggleShowPassword() {
    this.setData({ showPassword: !this.data.showPassword });
  },

  toggleShowConfirmPassword() {
    this.setData({ showConfirmPassword: !this.data.showConfirmPassword });
  },

  onSms(e) {
    this.setData({ "dataRegisterFrom.smsCode": this.pickVal(e) });
  },

  onSendSms() {
    const account = (this.data.dataRegisterFrom.account || "").trim();
    if (!account) {
      wx.showToast({ title: "请先填写手机号", icon: "none" });
      return;
    }
    if (!/^1[3-9]\d{9}$/.test(account)) {
      wx.showToast({ title: "请输入正确手机号", icon: "none" });
      return;
    }
    sendRegisterSms(account)
      .then((info) => {
        if (info.code === 0) {
          wx.showToast({ title: "已发送(模拟)", icon: "none" });
          this.startSmsCooldown(55);
        } else {
          wx.showToast({ title: info.msg || "发送失败", icon: "none" });
        }
      })
      .catch((e) => {
        let msg =
          (e && e.errMsg) ||
          (e && e.__http && e.errMsg) ||
          "网络异常：请检查 utils/config.js 里 baseUrl（须能访问到 Java 的 /self-study）";
        if (msg.indexOf("request:fail") >= 0) {
          msg = "连不上后端：请启动 studyroom-java，并把 baseUrl 改为本机 IP 或 127.0.0.1:8081/self-study（与 server.port 一致）";
        }
        wx.showModal({
          title: "发送失败",
          content: msg,
          showCancel: false,
        });
      });
  },

  startSmsCooldown(sec) {
    this.setData({ smsSeconds: sec, smsBtnText: sec + "s" });
    const t = setInterval(() => {
      const s = this.data.smsSeconds - 1;
      if (s <= 0) {
        clearInterval(t);
        this.setData({ smsSeconds: 0, smsBtnText: "获取验证码" });
      } else {
        this.setData({ smsSeconds: s, smsBtnText: s + "s" });
      }
    }, 1000);
  },

  registration() {
    const f = this.data.dataRegisterFrom;
    if (!f.account || !f.password || !f.confirmPassword || !f.smsCode) {
      wx.showToast({ title: "请填写完整", icon: "none" });
      return;
    }
    if (f.password !== f.confirmPassword) {
      wx.showToast({ title: "两次密码不一致", icon: "none" });
      return;
    }
    register({
      account: f.account.trim(),
      password: f.password,
      confirmPassword: f.confirmPassword,
      smsCode: f.smsCode.trim(),
    }).then((info) => {
      if (info.code == 0) {
        wx.showToast({
          title: "注册成功",
          icon: "success",
          duration: 1200,
        });
        const account = f.account.trim();
        const password = f.password;
        login({ account, password })
          .then((loginInfo) => {
            const token =
              loginInfo.token ||
              (loginInfo.data && loginInfo.data.token);
            if (loginInfo.code === 0 && token) {
              wx.setStorageSync("token", token);
              wx.switchTab({ url: pages.My });
            } else {
              wx.showToast({
                title: loginInfo.msg || "请登录",
                icon: "none",
              });
              wx.navigateTo({ url: pages.Login });
            }
          })
          .catch(() => {
            wx.navigateTo({ url: pages.Login });
          });
      } else {
        wx.showToast({ title: info.msg || "注册失败", icon: "none" });
      }
    }).catch((e) => {
      let msg = (e && e.errMsg) || "网络异常";
      if (msg.indexOf("request:fail") >= 0) {
        msg = "连不上后端：请检查 utils/config.js 的 baseUrl 与后端是否已启动";
      }
      wx.showModal({ title: "注册失败", content: msg, showCancel: false });
    });
  },

  onLoad() {},
  onReady() {},
  onShow() {},
  onHide() {},
  onUnload() {},
  onPullDownRefresh() {},
  onReachBottom() {},
  onShareAppMessage() {},
});
