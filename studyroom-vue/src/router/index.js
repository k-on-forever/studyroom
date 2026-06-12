/**
 * 全站路由配置
 *
 * 建议:
 * 1. 代码中路由统一使用name属性跳转(不使用path属性)
 */
import Vue from 'vue'
import Router from 'vue-router'
import http from '@/utils/httpRequest'
import { isURL } from '@/utils/validate'
import { clearLoginInfo } from '@/utils'

Vue.use(Router)

// 开发环境不使用懒加载, 因为懒加载页面太多的话会造成webpack热更新太慢, 所以只有生产环境使用懒加载
const _import = require('./import-' + process.env.NODE_ENV)

// 动态菜单注册后仍保留的固定子路由（避免后端菜单为空时只剩空白页）
const mainFixedChildren = [
  { path: '/home', component: _import('common/home'), name: 'home', meta: { title: '首页' } },
  { path: '/bas/analytics-revenue', component: _import('modules/bas/analytics-revenue'), name: 'bas-analytics-revenue', meta: { title: '营收统计' } },
  { path: '/bas/analytics-util', component: _import('modules/bas/analytics-utilization'), name: 'bas-analytics-util', meta: { title: '座位利用率' } },
  { path: '/bas/seat-editor', component: _import('modules/bas/seat-editor'), name: 'bas-seat-editor', meta: { title: '座位编辑器' } },
  { path: '/bas/sys-log', component: _import('modules/bas/sys-log'), name: 'bas-sys-log', meta: { title: '操作日志' } },
  { path: '/theme', component: _import('common/theme'), name: 'theme', meta: { title: '主题' } }
]

// 全局路由(无需嵌套上左右整体布局)
const globalRoutes = [
  { path: '/404', component: _import('common/404'), name: '404', meta: { title: '404未找到' } },
  { path: '/login', component: _import('common/login'), name: 'login', meta: { title: '登录' } }
]

// 主入口路由(需嵌套上左右整体布局)
const mainRoutes = {
  path: '/',
  component: _import('main'),
  name: 'main',
  redirect: { name: 'home' },
  meta: { title: '主入口整体布局' },
  children: mainFixedChildren.slice(),
  beforeEnter (to, from, next) {
    let token = Vue.cookie.get('token')
    if (!token || !/\S/.test(token)) {
      clearLoginInfo()
      next({ name: 'login' })
      return
    }
    next()
  }
}

const router = new Router({
  mode: 'hash',
  scrollBehavior: () => ({ y: 0 }),
  isAddDynamicMenuRoutes: false, // 是否已经添加动态(菜单)路由
  routes: globalRoutes.concat(mainRoutes)
})

router.beforeEach((to, from, next) => {
  // 添加动态(菜单)路由
  // 1. 已经添加 or 全局路由, 直接访问
  // 2. 获取菜单列表, 添加并保存本地存储
  if (router.options.isAddDynamicMenuRoutes || fnCurrentRouteType(to, globalRoutes) === 'global') {
    next()
  } else {
    http({
      url: http.adornUrl('/sys/menu/nav'),
      method: 'get',
      params: http.adornParams()
    }).then(({data}) => {
      if (data && data.code === 401) {
        clearLoginInfo()
        sessionStorage.setItem('menuList', '[]')
        sessionStorage.setItem('permissions', '[]')
        next({ name: 'login', replace: true })
        return
      }
      if (data && data.code === 0) {
        fnAddDynamicMenuRoutes(data.menuList)
        router.options.isAddDynamicMenuRoutes = true
        sessionStorage.setItem('menuList', JSON.stringify(data.menuList || '[]'))
        sessionStorage.setItem('permissions', JSON.stringify(data.permissions || '[]'))
        next({ ...to, replace: true })
      } else {
        sessionStorage.setItem('menuList', '[]')
        sessionStorage.setItem('permissions', '[]')
        next()
      }
    }).catch((e) => {
      console.warn('请求菜单列表失败:', e)
      clearLoginInfo()
      sessionStorage.setItem('menuList', '[]')
      sessionStorage.setItem('permissions', '[]')
      next({ name: 'login', replace: true })
    })
  }
})

/**
 * 判断当前路由类型, global: 全局路由, main: 主入口路由
 * @param {*} route 当前路由
 */
function fnCurrentRouteType (route, globalRoutes = []) {
  var temp = []
  for (var i = 0; i < globalRoutes.length; i++) {
    if (route.path === globalRoutes[i].path) {
      return 'global'
    } else if (globalRoutes[i].children && globalRoutes[i].children.length >= 1) {
      temp = temp.concat(globalRoutes[i].children)
    }
  }
  return temp.length >= 1 ? fnCurrentRouteType(route, temp) : 'main'
}

/**
 * 添加动态(菜单)路由
 * @param {*} menuList 菜单列表
 * @param {*} routes 递归创建的动态(菜单)路由
 */
function fnAddDynamicMenuRoutes (menuList = [], routes = []) {
  var temp = []
  for (var i = 0; i < menuList.length; i++) {
    if (menuList[i].list && menuList[i].list.length >= 1) {
      temp = temp.concat(menuList[i].list)
    } else if (menuList[i].url && /\S/.test(menuList[i].url)) {
      menuList[i].url = menuList[i].url.replace(/^\//, '')
      var route = {
        path: menuList[i].url.replace('/', '-'),
        component: null,
        name: menuList[i].url.replace('/', '-'),
        meta: {
          menuId: menuList[i].menuId,
          title: menuList[i].name,
          isDynamic: true,
          isTab: true,
          iframeUrl: ''
        }
      }
      // url以http[s]://开头, 通过iframe展示
      if (isURL(menuList[i].url)) {
        route['path'] = `i-${menuList[i].menuId}`
        route['name'] = `i-${menuList[i].menuId}`
        route['meta']['iframeUrl'] = menuList[i].url
      } else {
        try {
          route['component'] = _import(`modules/${menuList[i].url}`) || null
        } catch (e) {}
      }
      routes.push(route)
    }
  }
  if (temp.length >= 1) {
    fnAddDynamicMenuRoutes(temp, routes)
  } else {
    mainRoutes.children = mainFixedChildren.concat(routes)
    sessionStorage.setItem('dynamicMenuRoutes', JSON.stringify(mainRoutes.children || '[]'))
    console.log('\n')
    console.log('%c!<-------------------- 动态(菜单)路由 s -------------------->', 'color:blue')
    console.log(mainRoutes.children)
    console.log('%c!<-------------------- 动态(菜单)路由 e -------------------->', 'color:blue')

    if (routes.length === 0) {
      // 后端菜单为空时，初始路由里已有 main + home/theme，不能再 addRoutes(mainRoutes)，否则会重复注册同名路由并告警
      if (!router.options.__fallback404Added) {
        router.addRoutes([{ path: '*', redirect: { name: '404' } }])
        router.options.__fallback404Added = true
      }
    } else {
      // 有动态菜单时不能再 addRoutes(mainRoutes)：会与初始路由里已注册的 home/theme 重名。
      // 用 matcher 整体替换为「固定子路由 + 动态路由」一份表。
      const mainWithChildren = {
        path: '/',
        component: mainRoutes.component,
        name: 'main',
        redirect: mainRoutes.redirect,
        meta: mainRoutes.meta,
        beforeEnter: mainRoutes.beforeEnter,
        children: mainFixedChildren.concat(routes)
      }
      const rebuilt = new Router({
        mode: router.mode,
        scrollBehavior: router.options.scrollBehavior,
        routes: globalRoutes.concat([
          mainWithChildren,
          { path: '*', redirect: { name: '404' } }
        ])
      })
      router.matcher = rebuilt.matcher
      router.options.routes = rebuilt.options.routes
      mainRoutes.children = mainWithChildren.children
      if (!router.options.__fallback404Added) {
        router.options.__fallback404Added = true
      }
    }
  }
}

export default router
