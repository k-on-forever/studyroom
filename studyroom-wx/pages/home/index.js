const { getAnnounceList } = require("../../api/announce");

/** 首页轮播（勿单独 require utils 下仅赋值 exports.xxx 的文件，部分基础库会报 module not defined） */
const HOME_BANNER_URLS = [
  "https://s1.ax1x.com/2023/02/01/pSBz3Y4.jpg",
  "https://s1.ax1x.com/2023/02/01/pSBz1kF.jpg",
  "https://s1.ax1x.com/2023/02/01/pSBz8fJ.jpg",
  "https://s1.ax1x.com/2023/02/01/pSBzQTU.jpg",
];

Page({
  data: {
    notices: [],
    /** 请求结束后用于展示「暂无公告」空态 */
    noticesLoaded: false,
    bannerUrls: HOME_BANNER_URLS,
  },

  onLoad() {
    this.loadNotices();
  },

  onShow() {
    if (typeof this.getTabBar === "function" && this.getTabBar()) {
      this.getTabBar().setData({ selected: 0 });
    }
  },

  loadNotices() {
    getAnnounceList(1, 5, "")
      .then((info) => {
        if (info.code !== 0 || !info.data) {
          this.setData({ notices: [], noticesLoaded: true });
          return;
        }
        const raw =
          info.data.list || info.data.content || info.data.records || [];
        this.setData({
          notices: Array.isArray(raw) ? raw : [],
          noticesLoaded: true,
        });
      })
      .catch(() => {
        this.setData({ notices: [], noticesLoaded: true });
      });
  },

  goRegion() {
    wx.navigateTo({ url: "/subpackages/pages/region/index" });
  },

  goAnnounce() {
    wx.navigateTo({ url: "/subpackages/pages/announce/index" });
  },
});
