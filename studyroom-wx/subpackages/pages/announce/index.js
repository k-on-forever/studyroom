const { getAnnounceList } = require("../../../api/announce");

Page({
  data: {
    dataList: [],
    loading: true,
    loadError: "",
  },

  onLoad() {
    this.loadNotices();
  },

  onPullDownRefresh() {
    this.loadNotices().finally(() => wx.stopPullDownRefresh());
  },

  loadNotices() {
    this.setData({ loading: true, loadError: "" });
    return getAnnounceList(1, 30, "")
      .then((info) => {
        if (info.code !== 0) {
          this.setData({
            loading: false,
            loadError: info.msg || "加载失败",
            dataList: [],
          });
          return;
        }
        const pr = info.data || {};
        const list = pr.list || pr.content || pr.records || [];
        this.setData({
          loading: false,
          dataList: Array.isArray(list) ? list : [],
          loadError: "",
        });
      })
      .catch(() => {
        this.setData({
          loading: false,
          loadError: "网络异常，请检查后端与 baseUrl",
          dataList: [],
        });
      });
  },
});
