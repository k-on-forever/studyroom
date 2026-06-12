/**
 * 管理端后端地址（与 studyroom-java 的 server.servlet.context-path 一致，须带 /self-study）。
 *
 * 覆盖方式（任选其一，在加载本脚本之前执行）：
 *   window.__BACKEND_BASE__ = 'https://你的域名/self-study'
 * 部署后也可直接改下面 defaultBase 的默认值。
 */
;(function () {
  window.SITE_CONFIG = window.SITE_CONFIG || {}

  var defaultBase = '/self-study'
  var base =
    (typeof window !== 'undefined' && window.__BACKEND_BASE__) ||
    defaultBase
  window.SITE_CONFIG['baseUrl'] = base

  window.SITE_CONFIG['domain'] = './'
  window.SITE_CONFIG['version'] = ''
  window.SITE_CONFIG['cdnUrl'] = window.SITE_CONFIG.domain + window.SITE_CONFIG.version
})()
