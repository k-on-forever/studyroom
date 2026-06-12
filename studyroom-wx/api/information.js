const { request } = require("../utils/http");

function userInfo() {
  return request("/applet/getUser", { method: "GET" });
}

function userUpdate(body) {
  return request("/applet/userInfo", { method: "POST", data: body });
}

/** 头像 multipart，需在小程序后台配置 uploadFile 合法域名 */
function uploadAvatar(filePath) {
  const config = require("../utils/config");
  const token = wx.getStorageSync("token") || "";
  return new Promise((resolve, reject) => {
    wx.uploadFile({
      url: config.baseUrl + "/applet/upload/avatar",
      filePath,
      name: "file",
      header: token ? { token } : {},
      success(res) {
        const sc = res.statusCode;
        if (sc < 200 || sc >= 300) {
          reject(new Error("HTTP " + sc));
          return;
        }
        let body = res.data;
        if (typeof body === "string") {
          try {
            body = JSON.parse(body);
          } catch (e) {
            reject(new Error("响应非 JSON"));
            return;
          }
        }
        resolve(body);
      },
      fail(err) {
        reject(err || new Error("upload fail"));
      },
    });
  });
}

module.exports = { userInfo, userUpdate, uploadAvatar };
