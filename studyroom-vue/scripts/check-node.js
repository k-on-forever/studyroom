/**
 * 管理端构建需 Node 18+（推荐 22 LTS）。Webpack 5 + webpack-dev-server 5 已替代旧版 spdy 栈。
 */
var major = parseInt(process.versions.node.split('.')[0], 10);
if (major < 18) {
  console.error('');
  console.error('[studyroom-vue] 当前 Node 版本: ' + process.version);
  console.error('请使用 Node 18 或更高版本（推荐 22 LTS）。');
  console.error('Windows 示例: nvm install 22 && nvm use 22');
  console.error('然后在本目录执行: npm install && npm run dev');
  console.error('');
  process.exit(1);
}
