const { getStudyStats } = require("../../../api/studyStats");

Page({
  data: {
    hasToken: false,
    totalHours: "0",
    totalMinutesRemain: 0,
    streakDays: 0,
    heatmapDays: [],
  },

  fmtDuration(totalMinutes) {
    const m = Math.max(0, parseInt(totalMinutes, 10) || 0);
    const h = Math.floor(m / 60);
    const r = m % 60;
    return { h: String(h), r };
  },

  load() {
    const token = wx.getStorageSync("token");
    if (!token) {
      this.setData({
        hasToken: false,
        totalHours: "0",
        totalMinutesRemain: 0,
        streakDays: 0,
        heatmapDays: [],
      });
      return;
    }
    this.setData({ hasToken: true });
    getStudyStats()
      .then((info) => {
        if (info.code !== 0 || !info.data) {
          wx.showToast({ title: (info && info.msg) || "加载失败", icon: "none" });
          return;
        }
        const d = info.data;
        const tm = d.totalMinutes != null ? d.totalMinutes : 0;
        const fd = this.fmtDuration(tm);
        const cells = Array.isArray(d.heatmapDays) ? d.heatmapDays : [];
        this.setData({
          totalHours: fd.h,
          totalMinutesRemain: fd.r,
          streakDays: d.streakDays != null ? d.streakDays : 0,
          heatmapDays: cells,
        });
      })
      .catch(() => {
        wx.showToast({ title: "网络异常", icon: "none" });
      });
  },

  onShow() {
    this.load();
  },
});
