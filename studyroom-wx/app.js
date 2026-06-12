// app.js
App({
  onLaunch() {
    const logs = wx.getStorageSync('logs') || []
    logs.unshift(Date.now())
    wx.setStorageSync('logs', logs)

    setTimeout(() => {
      wx.login({
        success() {},
        fail() {},
      })
    }, 200)
  },
  globalData: {
    userInfo: null
  }
})
