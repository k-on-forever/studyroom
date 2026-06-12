const pages = require("../../../pages/index.js");
const {
  sendResetPasswordSms,
  resetPassword,
} = require("../../../api/password");

Page({
  data: {
    form: {
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

  toggleShowPassword() {
    this.setData({ showPassword: !this.data.showPassword });
  },

  toggleShowConfirmPassword() {
    this.setData({ showConfirmPassword: !this.data.showConfirmPassword });
  },

  pickVal(e) {
    const d = e.detail;
    if (d == null) return "";
    if (typeof d === "string" || typeof d === "number") return String(d);
    if (typeof d === "object" && d.value != null) return String(d.value);
    return "";
  },

  onAccount(e) {
    this.setData({ "form.account": this.pickVal(e) });
  },

  onPassword(e) {
    this.setData({ "form.password": this.pickVal(e) });
  },

  onConfirmPassword(e) {
    this.setData({ "form.confirmPassword": this.pickVal(e) });
  },

  onSms(e) {
    this.setData({ "form.smsCode": this.pickVal(e) });
  },

  onSendSms() {
    const account = (this.data.form.account || "").trim();
    if (!account) {
      wx.showToast({ title: "请先填写手机号", icon: "none" });
      return;
    }
    if (!/^1[3-9]\d{9}$/.test(account)) {
      wx.showToast({ title: "请输入正确手机号", icon: "none" });
      return;
    }
    sendResetPasswordSms(account)
      .then((info) => {
        if (info.code === 0) {
          wx.showToast({ title: "已发送(模拟)", icon: "none" });
          this.startSmsCooldown(55);
        } else {
          wx.showToast({ title: info.msg || "发送失败", icon: "none" });
        }
      })
      .catch(() => {
        wx.showToast({ title: "网络异常", icon: "none" });
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

  onSubmit() {
    const f = this.data.form;
    if (!f.account || !f.password || !f.confirmPassword || !f.smsCode) {
      wx.showToast({ title: "请填写完整", icon: "none" });
      return;
    }
    if (f.password !== f.confirmPassword) {
      wx.showToast({ title: "两次密码不一致", icon: "none" });
      return;
    }
    resetPassword({
      account: f.account.trim(),
      password: f.password,
      confirmPassword: f.confirmPassword,
      smsCode: f.smsCode.trim(),
    }).then((info) => {
      if (info.code == 0) {
        wx.showToast({ title: "已重置，请登录", icon: "success", duration: 2000 });
        setTimeout(() => {
          wx.navigateTo({ url: pages.Login });
        }, 500);
      } else {
        wx.showToast({ title: info.msg || "重置失败", icon: "none" });
      }
    });
  },
});
