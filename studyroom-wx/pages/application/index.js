const pages = require("../index.js");
const { formatHttpError } = require("../../utils/http");
const { enrichForList, bizDateOf } = require("../../utils/appointmentFormat");
const { mineWhitelist } = require("../../api/seatAccessRule");
import {
  getLicationList,
  cancelLication,
  overLication,
  signInLication,
} from "../../api/application";

const ACTIVE_POLL_MS = 45000;

function todayYmd() {
  const d = new Date();
  const y = d.getFullYear();
  const m = String(d.getMonth() + 1).padStart(2, "0");
  const day = String(d.getDate()).padStart(2, "0");
  return y + "-" + m + "-" + day;
}

function isUpcomingActive(it, today) {
  const bd = bizDateOf(it);
  if (it.seatState === 1) return true;
  if (it.seatState !== 0) return false;
  if (!bd) return true;
  return bd >= today;
}

function formatStoreAuthRule(r) {
  const place = [r.floor, r.roomName, r.seatName].filter(Boolean).join(" · ");
  const dates =
    r.dateFrom && r.dateTo && r.dateFrom !== r.dateTo
      ? r.dateFrom + " 至 " + r.dateTo
      : r.dateFrom || r.dateTo || "";
  const times =
    r.timeFrom && r.timeTo ? r.timeFrom + "–" + r.timeTo : "";
  return {
    place: place || "座位 " + (r.seatId || ""),
    when: [dates, times].filter(Boolean).join(" · "),
    remark: r.remark || "",
  };
}

Page({
  data: {
    makeFlag: true,
    listEmpty: false,
    emptyText: "加载中…",
    dataList: [],
    showGuide: false,
    hasToken: false,
    showRetry: false,
    listHint: "",
    storeAuthMobile: "",
    storeAuthHint: "",
    storeAuthRules: [],
  },

  _pollTimer: null,

  onCancelBook(e) {
    const item = e.currentTarget.dataset.item;
    const that = this;
    wx.showModal({
      title: "取消预约",
      content: "确认取消该预约？取消后座位将释放。",
      success(res) {
        if (!res.confirm) return;
        cancelLication(item.id).then((info) => {
          if (info.code == 0) {
            wx.showToast({ title: "已取消", icon: "success", duration: 1500 });
            that.geDataList();
          } else {
            wx.showModal({
              title: "无法取消",
              content: info.msg || "取消失败",
              showCancel: false,
            });
          }
        }).catch((e) => {
          wx.showModal({
            title: "无法取消",
            content: formatHttpError(e, "取消失败，请稍后重试"),
            showCancel: false,
          });
        });
      },
    });
  },

  onSignIn(e) {
    const item = e.currentTarget.dataset.item;
    if (!item || !item.canSignIn) {
      wx.showToast({
        title: (item && item.signInHint) || "未到预约开始时间",
        icon: "none",
        duration: 2600,
      });
      return;
    }
    const that = this;
    signInLication(item.id).then((info) => {
      if (info.code == 0) {
        wx.showToast({ title: "签到成功", icon: "success", duration: 1500 });
        that.geDataList();
      } else {
        wx.showToast({ title: info.msg || "签到失败", icon: "none" });
      }
    }).catch((e) => {
      wx.showToast({ title: formatHttpError(e, "签到失败"), icon: "none" });
    });
  },

  onOverStudy(e) {
    const item = e.currentTarget.dataset.item;
    const that = this;
    wx.showModal({
      title: "签退",
      content: "确认签退？将结束本次学习并释放座位。",
      success(res) {
        if (!res.confirm) return;
        overLication(item.id).then((info) => {
          if (info.code == 0) {
            wx.showToast({ title: "已签退", icon: "success", duration: 1500 });
            that.geDataList();
          } else {
            wx.showToast({ title: info.msg || "签退失败", icon: "none" });
          }
        }).catch((e) => {
          wx.showToast({ title: formatHttpError(e, "签退失败"), icon: "none" });
        });
      },
    });
  },

  goBookingDetail(e) {
    const id = e.currentTarget.dataset.id;
    if (!id) return;
    wx.navigateTo({
      url: pages.BookingDetail + "?id=" + encodeURIComponent(id),
    });
  },

  goLogin() {
    wx.navigateTo({ url: pages.Login });
  },

  goRegister() {
    wx.navigateTo({ url: pages.Register });
  },

  goMembershipBuy() {
    wx.navigateTo({ url: pages.MembershipBuy });
  },

  goMembershipMine() {
    wx.navigateTo({ url: pages.MembershipMine });
  },

  goHomeReserve() {
    wx.switchTab({ url: pages.Home });
  },

  goBookingRecord() {
    wx.navigateTo({ url: pages.BookingRecord });
  },

  loadStoreAuth() {
    mineWhitelist()
      .then((info) => {
        if (info.code !== 0 || !info.data) {
          this.setData({
            storeAuthMobile: "",
            storeAuthHint: "",
            storeAuthRules: [],
          });
          return;
        }
        const d = info.data;
        const rules = Array.isArray(d.rules)
          ? d.rules.map(formatStoreAuthRule)
          : [];
        this.setData({
          storeAuthMobile: d.mobileMask || "",
          storeAuthHint: d.credentialHint || "",
          storeAuthRules: rules,
        });
      })
      .catch(() => {
        this.setData({
          storeAuthMobile: "",
          storeAuthHint: "",
          storeAuthRules: [],
        });
      });
  },

  geDataList(silent) {
    const token = wx.getStorageSync("token");
    const ymd = todayYmd();
    if (!silent) {
      this.setData({
        hasToken: !!token,
        showRetry: false,
        makeFlag: true,
        listEmpty: false,
        emptyText: token ? "加载中…" : "登录后可查看预约并选座",
      });
    }
    if (!token) {
      this.setData({
        dataList: [],
        makeFlag: true,
        listEmpty: false,
        emptyText: "请先登录或注册，再使用入座与选座功能",
        showGuide: true,
        hasToken: false,
      });
      this.stopActivePoll();
      return;
    }
    getLicationList()
      .then((info) => {
        if (info.code === 0) {
          const list = Array.isArray(info.data) ? info.data : [];
          const upcoming = list
            .filter((it) => isUpcomingActive(it, ymd))
            .map(enrichForList)
            .sort((a, b) => {
              const d = (a.bizDate || "").localeCompare(b.bizDate || "");
              if (d !== 0) return d;
              return (a.id || 0) - (b.id || 0);
            });
          const makeFlag = list.length === 0;
          const listEmpty = !makeFlag && upcoming.length === 0;
          this.setData({
            dataList: upcoming,
            makeFlag,
            listEmpty,
            listHint: makeFlag
              ? ""
              : "共 " +
                upcoming.length +
                " 条待签到/使用中 · 到点未签退将自动结束",
            emptyText: makeFlag ? "暂无预约，可按下方步骤开始" : "",
            showGuide: makeFlag,
            showRetry: false,
            hasToken: true,
          });
          this.syncActivePoll(upcoming);
        } else if (info.code === 401) {
          this.setData({
            dataList: [],
            makeFlag: true,
            listEmpty: false,
            emptyText: "登录已失效，请重新登录",
            showGuide: true,
            hasToken: false,
          });
          this.stopActivePoll();
        } else if (!silent) {
          this.setData({
            dataList: [],
            makeFlag: true,
            listEmpty: false,
            emptyText: info.msg || "加载失败",
            showGuide: true,
            showRetry: true,
          });
        }
      })
      .catch((e) => {
        if (!silent) {
          this.setData({
            dataList: [],
            makeFlag: true,
            listEmpty: false,
            emptyText: formatHttpError(
              e,
              "无法连接后端。请确认 utils/config.js 的 baseUrl（真机请用电脑局域网 IP）"
            ),
            showGuide: true,
            showRetry: true,
          });
        }
      });
  },

  syncActivePoll(list) {
    const hasActive =
      list && list.some((it) => it.seatState === 0 || it.seatState === 1);
    if (hasActive) {
      this.startActivePoll();
    } else {
      this.stopActivePoll();
    }
  },

  startActivePoll() {
    this.stopActivePoll();
    this._pollTimer = setInterval(() => {
      if (this.data.hasToken) {
        this.geDataList(true);
      }
    }, ACTIVE_POLL_MS);
  },

  stopActivePoll() {
    if (this._pollTimer != null) {
      clearInterval(this._pollTimer);
      this._pollTimer = null;
    }
  },

  onLoad() {},

  onShow() {
    this.geDataList();
    this.loadStoreAuth();
    if (typeof this.getTabBar === "function" && this.getTabBar()) {
      this.getTabBar().setData({ selected: 1 });
    }
  },

  onHide() {
    this.stopActivePoll();
  },

  onUnload() {
    this.stopActivePoll();
  },
});
