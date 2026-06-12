// subpackages/pages/information/index.js
const pages = require("../../../pages/index.js");
const { userInfo, userUpdate, uploadAvatar } = require("../../../api/information");
Page({
  /**
   * 页面的初始数据
   */
  data: {
    wordCount: 0,
    dataFrom: {
      mobile: "",
      name: "",
      userImg: "",
      email: "",
      qq: "",
      bz: "",
    },
    //信息输入状态
    fromShow: false,
    min: 0,
    max: 100,
  },
  // 提交（与后端 UserInfoUpdateDTO 一致：邮箱须含 @；手机 11 位；QQ 为数字）
  regionCheck() {
    const d = this.data.dataFrom;
    const name = (d.name || "").trim();
    const mobile = (d.mobile || "").trim();
    const email = (d.email || "").trim();
    const bz = (d.bz || "").trim();
    const qqStr = d.qq != null && d.qq !== "" ? String(d.qq).trim() : "";
    if (mobile && !/^1[3-9]\d{9}$/.test(mobile)) {
      wx.showToast({ title: "手机号须为 11 位大陆号", icon: "none" });
      return;
    }
    if (email && !/^[^\s@]{1,64}@[^\s@]{1,255}\.[^\s@.]{2,}$/.test(email)) {
      wx.showToast({ title: "邮箱格式不正确", icon: "none" });
      return;
    }
    if (qqStr && !/^\d{5,12}$/.test(qqStr)) {
      wx.showToast({ title: "QQ 号为 5～12 位数字", icon: "none" });
      return;
    }
    if (bz.length > 100) {
      wx.showToast({ title: "备注最多 100 字", icon: "none" });
      return;
    }
    const payload = {
      userImg: d.userImg || "",
      name,
      mobile,
      email,
      bz,
    };
    if (qqStr) {
      payload.qq = Number(qqStr);
    }
    userUpdate(payload)
      .then((info) => {
        if (info.code == 0) {
          wx.showToast({
            title: "修改成功",
            icon: "success",
            duration: 2000,
          });
          setTimeout(() => {
            wx.switchTab({
              url: pages.My,
            });
          }, 500);
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
          wx.showToast({ title: (info && info.msg) || "保存失败", icon: "none" });
        }
      })
      .catch(() => {
        wx.showToast({ title: "网络异常", icon: "none" });
      });
  },
  inputeExplain(e) {
    const v = (e.detail && e.detail.value) || "";
    const max = this.data.max != null ? this.data.max : 100;
    const slice = v.length > max ? v.slice(0, max) : v;
    this.setData({
      "dataFrom.bz": slice,
      wordCount: slice.length,
    });
  },

  onChange(e) {
    const sign = e.currentTarget.dataset.sign;
    const v = (e.detail && e.detail.value) || "";
    if (sign === "name") {
      this.setData({ "dataFrom.name": v });
    }
    if (sign === "qq") {
      this.setData({ "dataFrom.qq": v });
    }
    if (sign === "email") {
      this.setData({ "dataFrom.email": v });
    }
    if (sign === "mobile") {
      this.setData({ "dataFrom.mobile": v });
    }
  },

  /** 相册/拍照上传头像，成功后写入 dataFrom.userImg（点「提交」再落库） */
  chooseAvatar() {
    if (!wx.getStorageSync("token")) {
      wx.showToast({ title: "请先登录", icon: "none" });
      return;
    }
    wx.chooseMedia({
      count: 1,
      mediaType: ["image"],
      sourceType: ["album", "camera"],
      success: (res) => {
        const f = res.tempFiles && res.tempFiles[0];
        const path = f && f.tempFilePath;
        if (!path) {
          return;
        }
        wx.showLoading({ title: "上传中", mask: true });
        uploadAvatar(path)
          .then((body) => {
            wx.hideLoading();
            if (body.code === 0 && body.userImg) {
              this.setData({ "dataFrom.userImg": body.userImg });
              wx.showToast({ title: "头像已更新", icon: "success" });
            } else {
              wx.showToast({
                title: (body && body.msg) || "上传失败",
                icon: "none",
              });
            }
          })
          .catch(() => {
            wx.hideLoading();
            wx.showToast({
              title: "上传失败：请检查网络与 uploadFile 合法域名",
              icon: "none",
              duration: 3200,
            });
          });
      },
    });
  },
  /**
   * 生命周期函数--监听页面加载
   */
  onLoad(options) {
    userInfo()
      .then((info) => {
        if (info.code === 0 && info.data) {
          const d = Object.assign({ bz: "" }, info.data);
          this.setData({
            dataFrom: d,
            wordCount: (d.bz && d.bz.length) || 0,
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
          wx.showToast({ title: (info && info.msg) || "加载失败", icon: "none" });
        }
      })
      .catch(() => {
        wx.showToast({ title: "网络异常", icon: "none" });
      });
  },

  /**
   * 生命周期函数--监听页面初次渲染完成
   */
  onReady() {},

  /**
   * 生命周期函数--监听页面显示
   */
  onShow() {},

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
