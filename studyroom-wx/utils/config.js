/**
 * 后端 context-path 为 /self-study（须与 studyroom-java 的 server.servlet.context-path 一致）。
 *
 * 模拟器 + 后端在本机：用 127.0.0.1 或 localhost 均可（若其一连不上可换另一个试）。
 * 真机调试 / 手机扫码预览：127.0.0.1 指向手机自己，必须改成电脑的局域网 IP，例如
 *   http://192.168.1.8:8081/self-study
 * 电脑 IP 可在 cmd 执行 ipconfig 查看 IPv4；后端 application.yml 已 server.address=0.0.0.0 时局域网可访问。
 * 端口须与 studyroom-java 的 server.port 一致（当前默认 8081）。
 */
module.exports = {
  baseUrl: "http://127.0.0.1:8081/self-study",
};
