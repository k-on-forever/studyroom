/**
 * 统一 API 请求工具 —— 减少各页面重复的 axios + 错误处理模板代码。
 *
 * 用法：
 *   import { apiGet, apiPost } from '@/utils/api'
 *   const data = await apiGet(this, '/sys/bas/seat/list', { roomId: 1 })
 */
import { httpConnErrorMessage } from '@/utils'

/**
 * 通用 GET 请求
 * @param {Vue} vm - 组件实例 (this)
 * @param {string} url - 后端路径 (不含 baseUrl 前缀)
 * @param {object} [params] - 查询参数
 * @returns {Promise<any>} 后端 data 字段
 */
export function apiGet (vm, url, params = {}) {
  return vm.$http({
    url: vm.$http.adornUrl(url),
    method: 'get',
    params: vm.$http.adornParams(params)
  }).then(({ data }) => {
    if (data && data.code === 0) {
      return data.data
    }
    const msg = (data && data.msg) || '请求失败'
    vm.$message.error(msg)
    return Promise.reject(new Error(msg))
  }).catch((e) => {
    vm.$message.error(httpConnErrorMessage(e))
    return Promise.reject(e)
  })
}

/**
 * 通用 POST 请求
 * @param {Vue} vm
 * @param {string} url
 * @param {object} [body] - 请求体
 * @returns {Promise<any>}
 */
export function apiPost (vm, url, body = {}) {
  return vm.$http({
    url: vm.$http.adornUrl(url),
    method: 'post',
    data: vm.$http.adornData(body)
  }).then(({ data }) => {
    if (data && data.code === 0) {
      return data.data
    }
    const msg = (data && data.msg) || '操作失败'
    vm.$message.error(msg)
    return Promise.reject(new Error(msg))
  }).catch((e) => {
    vm.$message.error(httpConnErrorMessage(e))
    return Promise.reject(e)
  })
}

/**
 * 静默 GET（不弹错误提示，适合轮询/后台加载）
 */
export function apiGetSilent (vm, url, params = {}) {
  return vm.$http({
    url: vm.$http.adornUrl(url),
    method: 'get',
    params: vm.$http.adornParams(params)
  }).then(({ data }) => {
    if (data && data.code === 0) return data.data
    return Promise.reject(new Error((data && data.msg) || '请求失败'))
  })
}
