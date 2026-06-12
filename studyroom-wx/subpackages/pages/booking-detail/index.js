const {
  getAppointmentDetail,
  cancelLication,
  overLication,
  signInLication,
} = require("../../../api/application");
const { enrichForDetail } = require("../../../utils/appointmentFormat");

const DETAIL_POLL_MS = 45000;

Page({
  data: {
    id: null,
    detail: null,
    stateLabel: "",
    loadErr: "",
  },

  _pollTimer: null,

  applyDetail(raw) {
    const d = enrichForDetail(raw);
    this.setData({
      detail: d,
      stateLabel: d.stateLabel,
    });
    this.syncDetailPoll(d);
  },

  syncDetailPoll(d) {
    if (d && d.seatState === 1) {
      this.startDetailPoll();
    } else {
      this.stopDetailPoll();
    }
  },

  startDetailPoll() {
    this.stopDetailPoll();
    this._pollTimer = setInterval(() => {
      this.reload(true);
    }, DETAIL_POLL_MS);
  },

  stopDetailPoll() {
    if (this._pollTimer != null) {
      clearInterval(this._pollTimer);
      this._pollTimer = null;
    }
  },

  reload(silent) {
    const id = this.data.id;
    if (!id) return;
    getAppointmentDetail(id)
      .then((info) => {
        if (info.code === 0 && info.data) {
          this.applyDetail(info.data);
          if (!silent) {
            this.setData({ loadErr: "" });
          }
        } else if (!silent) {
          this.setData({
            detail: null,
            loadErr: (info && info.msg) || "无法加载",
          });
          this.stopDetailPoll();
        }
      })
      .catch(() => {
        if (!silent) {
          this.setData({ detail: null, loadErr: "网络异常" });
          this.stopDetailPoll();
        }
      });
  },

  onLoad(options) {
    const id = options.id != null ? String(options.id).trim() : "";
    if (!id) {
      this.setData({ loadErr: "缺少预约 ID" });
      return;
    }
    this.setData({ id });
    this.reload();
  },

  onShow() {
    if (this.data.id) {
      this.reload();
    }
  },

  onHide() {
    this.stopDetailPoll();
  },

  onUnload() {
    this.stopDetailPoll();
  },

  onSignIn() {
    const d = this.data.detail || {};
    if (!d.canSignIn) {
      wx.showToast({
        title: d.signInHint || "未到预约开始时间",
        icon: "none",
        duration: 2600,
      });
      return;
    }
    const id = this.data.id;
    const that = this;
    signInLication(id).then((info) => {
      if (info.code == 0) {
        wx.showToast({ title: "签到成功", icon: "success" });
        that.reload();
      } else {
        wx.showToast({ title: info.msg || "签到失败", icon: "none" });
      }
    });
  },

  onCancel() {
    const id = this.data.id;
    const that = this;
    wx.showModal({
      title: "取消预约",
      content: "确认取消该预约？取消后座位将释放。",
      success(res) {
        if (!res.confirm) return;
        cancelLication(id).then((info) => {
          if (info.code == 0) {
            wx.showToast({ title: "已取消", icon: "success" });
            that.reload();
          } else {
            wx.showToast({ title: info.msg || "取消失败", icon: "none" });
          }
        });
      },
    });
  },

  onOver() {
    const id = this.data.id;
    const that = this;
    wx.showModal({
      title: "签退",
      content: "确认签退？将结束本次学习并释放座位。",
      success(res) {
        if (!res.confirm) return;
        overLication(id).then((info) => {
          if (info.code == 0) {
            wx.showToast({ title: "已签退", icon: "success" });
            that.reload();
          } else {
            wx.showToast({ title: info.msg || "操作失败", icon: "none" });
          }
        });
      },
    });
  },
});
