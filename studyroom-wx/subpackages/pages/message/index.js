const { addMessage } = require("../../../api/message");
const pages = require("../../../pages/index.js");
// subpackages/pages/message/index.js
Page({

  /**
   * 页面的初始数据
   */
  data: {
    choiceList: [
      {
        text: "异常反馈举报"
        
      },
      {
        text: "功能建议留言"
      }
    ],
    choiceIndex: 0,
    wordCount: 0,
    min: 0,
    max: 150,
    phoneValue: '',
    dataFrom: {
      messageType: 0,
      message: ""
    }
  },
  handChoice(row) {
    const idx = Number(row.currentTarget.dataset.index) || 0;
    this.setData({
      choiceIndex: idx,
      "dataFrom.messageType": idx,
    });
  },
  onMessageInput(e) {
    const v = (e.detail && e.detail.value) || "";
    const max = this.data.max;
    const slice = v.length > max ? v.slice(0, max) : v;
    this.setData({
      "dataFrom.message": slice,
      wordCount: slice.length,
    });
  },
  // 提交
  regionCheck() {
    if (!this.data.dataFrom.message) {
      wx.showToast({
        title: "请完善信息",
        icon: "none",
        duration: 2000,
      });
      return false
    }
    addMessage(this.data.dataFrom)
      .then((info) => {
        if (info.code == 0) {
          wx.showToast({
            title: '留言成功',
            icon: 'success',
            duration: 1500
          })
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
          wx.showToast({ title: (info && info.msg) || "提交失败", icon: "none" });
        }
      })
      .catch(() => {
        wx.showToast({ title: "网络异常", icon: "none" });
      });
  },
  /**
   * 生命周期函数--监听页面加载
   */
  onLoad(options) {

  },

  /**
   * 生命周期函数--监听页面初次渲染完成
   */
  onReady() {

  },

  /**
   * 生命周期函数--监听页面显示
   */
  onShow() {

  },

  /**
   * 生命周期函数--监听页面隐藏
   */
  onHide() {

  },

  /**
   * 生命周期函数--监听页面卸载
   */
  onUnload() {

  },

  /**
   * 页面相关事件处理函数--监听用户下拉动作
   */
  onPullDownRefresh() {

  },

  /**
   * 页面上拉触底事件的处理函数
   */
  onReachBottom() {

  },

  /**
   * 用户点击右上角分享
   */
  onShareAppMessage() {

  }
})