const config = require("./config");

/**
 * 将 wx.request 的失败对象转成可读文案（供各页 catch 里 showToast，避免把 HTTP 500 误叫成「网络异常」）。
 * @param {any} e
 * @param {string} [fallback]
 */
function formatHttpError(e, fallback) {
  if (!e) return fallback || "请求失败";
  if (e.__http) {
    return e.errMsg || "服务器错误(" + (e.statusCode || "?") + ")";
  }
  if (e.__net) {
    const m = (e.errMsg && String(e.errMsg)) || "";
    if (m.indexOf("CONNECTION_REFUSED") >= 0 || m.indexOf("connection refused") >= 0) {
      return "连接被拒绝：请确认 studyroom-java 已启动且 utils/config.js 端口与 server.port 一致";
    }
    if (m.indexOf("timeout") >= 0) {
      return "请求超时：后端未响应或网络较慢";
    }
    return m || fallback || "网络不可用";
  }
  return (e.errMsg && String(e.errMsg)) || fallback || "请求失败";
}

/**
 * @param {string} url 以 / 开头的路径，如 /applet/login
 * @param {WechatMiniprogram.RequestOption} opt
 */
function request(url, opt = {}) {
  const token = wx.getStorageSync("token") || "";
  return new Promise((resolve, reject) => {
    wx.request({
      url: config.baseUrl + url,
      method: opt.method || "GET",
      data: opt.data || {},
      /** 默认 60s；本地后端未启动时 fail 文案更清晰，避免工具链短时 timeout */
      timeout: opt.timeout != null ? opt.timeout : 60000,
      header: Object.assign(
        { "content-type": "application/json" },
        token ? { token } : {},
        opt.header || {}
      ),
      success(res) {
        const sc = res.statusCode;
        if (sc < 200 || sc >= 300) {
          var payload = res.data;
          var serverMsg = "";
          if (payload && typeof payload === "object" && payload.msg) {
            serverMsg = String(payload.msg);
          }
          var hint =
            serverMsg ||
            (sc >= 500
              ? "服务器内部错误(" +
                sc +
                ")：请查看 IDEA 控制台；常见原因为数据库缺列（如 bas_seat.seat_type），需执行 db/schema-studyroom-migration-admin-platform.sql"
              : "HTTP " + sc);
          reject({
            __http: true,
            statusCode: sc,
            errMsg: hint,
            data: res.data,
          });
          return;
        }
        const d = res.data;
        if (d == null || typeof d === "string") {
          reject({
            __http: true,
            statusCode: sc,
            errMsg: "服务器返回非 JSON（请检查 baseUrl 是否指向本服务 /self-study）",
            raw: typeof d === "string" ? d.slice(0, 200) : d,
          });
          return;
        }
        resolve(d);
      },
      fail(err) {
        reject({
          __net: true,
          errMsg: (err && err.errMsg) || "请求失败",
          err,
        });
      },
    });
  });
}

module.exports = { request, formatHttpError };
