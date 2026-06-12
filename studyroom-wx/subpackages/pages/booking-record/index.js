const pages = require("../../../pages/index.js");
const { formatHttpError } = require("../../../utils/http");
const { enrichForList } = require("../../../utils/appointmentFormat");
import { getLicationList } from "../../../api/application";

Page({
  data: {
    list: [],
    loading: true,
    err: "",
  },

  onLoad() {
    this.load();
  },

  onPullDownRefresh() {
    this.load(() => wx.stopPullDownRefresh());
  },

  load(done) {
    const token = wx.getStorageSync("token");
    if (!token) {
      this.setData({ loading: false, list: [], err: "请先登录" });
      done && done();
      return;
    }
    this.setData({ loading: true, err: "" });
    getLicationList()
      .then((info) => {
        if (info.code === 0) {
          const raw = Array.isArray(info.data) ? info.data : [];
          const list = raw.map((it) => {
            const row = enrichForList(it);
            return Object.assign({}, row, {
              stateText: row.stateText,
            });
          });
          this.setData({ list, loading: false, err: "" });
        } else {
          this.setData({
            loading: false,
            list: [],
            err: (info && info.msg) || "加载失败",
          });
        }
      })
      .catch((e) => {
        this.setData({
          loading: false,
          list: [],
          err: formatHttpError(e, "网络异常"),
        });
      })
      .finally(() => done && done());
  },

  goDetail(e) {
    const id = e.currentTarget.dataset.id;
    if (!id) return;
    wx.navigateTo({
      url: pages.BookingDetail + "?id=" + encodeURIComponent(id),
    });
  },
});
