import Vue from 'vue'
import axios from 'axios'
import router from '@/router'
import store from '@/store'
import qs from 'qs'
import merge from 'lodash/merge'
import { Message } from 'element-ui'

const http = axios.create({
  timeout: 1000 * 30,
  withCredentials: true,
  headers: {
    'Content-Type': 'application/json; charset=utf-8'
  }
})

/** 与 @/utils 中 clearLoginInfo 行为一致，避免 router ↔ httpRequest 循环依赖 */
function clearLoginInfoLocal () {
  Vue.cookie.delete('token')
  store.commit('resetStore')
  router.options.isAddDynamicMenuRoutes = false
}

function httpConnErrorMessageLocal (e) {
  const m = e && e.message ? String(e.message) : ''
  if (m === 'Network Error' || m.indexOf('ECONNREFUSED') >= 0) {
    return '无法连接后端：请先启动 studyroom-java，并核对 static/config/index.js（或 window.__BACKEND_BASE__）与端口、context-path /self-study 一致'
  }
  if (e && e.code === 'ECONNABORTED') {
    return '请求超时：后端未响应或网络较慢'
  }
  return m || '请求失败'
}

let lastGlobalToast = 0
function toastOnce (msg, type = 'error') {
  const now = Date.now()
  if (now - lastGlobalToast < 600) return
  lastGlobalToast = now
  Message[type]({ message: msg, duration: 4200 })
}

http.interceptors.request.use(config => {
  config.headers['token'] = Vue.cookie.get('token')
  return config
}, error => Promise.reject(error))

http.interceptors.response.use(response => {
  if (response.status === 401) {
    clearLoginInfoLocal()
    router.push({ name: 'login' })
    return Promise.reject(new Error('Unauthorized'))
  }
  const res = response.data
  if (res && res.code === 401) {
    clearLoginInfoLocal()
    router.push({ name: 'login' })
    return Promise.reject(new Error('Unauthorized'))
  }
  if (res && res.code === 403) {
    toastOnce((res && res.msg) || '无权操作')
    return Promise.reject(new Error('Forbidden'))
  }
  if (typeof res === 'string') {
    toastOnce('接口返回非 JSON，请检查 static/config/index.js 中 baseUrl 是否指向本后端(/self-study)')
    return Promise.reject(new Error('invalid response body'))
  }
  return response
}, error => {
  if (error.response) {
    const st = error.response.status
    const d = error.response.data
    const serverMsg = d && typeof d === 'object' && d.msg ? String(d.msg) : ''
    if (st === 401) {
      clearLoginInfoLocal()
      router.push({ name: 'login' })
      return Promise.reject(error)
    }
    if (serverMsg) {
      toastOnce(serverMsg)
      return Promise.reject(error)
    }
    if (st >= 500) {
      toastOnce('服务器错误(' + st + ')，请查看 studyroom-java 日志')
    }
  } else {
    toastOnce(httpConnErrorMessageLocal(error))
  }
  return Promise.reject(error)
})

http.adornUrl = (actionName) => {
  return (process.env.NODE_ENV !== 'production' && process.env.OPEN_PROXY ? '/proxyApi/' : window.SITE_CONFIG.baseUrl) + actionName
}

http.adornParams = (params = {}, openDefaultParams = true) => {
  var defaults = { 't': new Date().getTime() }
  return openDefaultParams ? merge(defaults, params) : params
}

http.adornData = (data = {}, openDefaultData = true, contentType = 'json') => {
  var defaults = { 't': new Date().getTime() }
  data = openDefaultData ? merge(defaults, data) : data
  return contentType === 'json' ? JSON.stringify(data) : qs.stringify(data)
}

export default http
