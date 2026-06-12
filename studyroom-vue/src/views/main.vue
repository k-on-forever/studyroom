<template>
  <div
    class="site-wrapper site-page--layout"
    :class="{
      'site-sidebar--fold': sidebarFold,
      'site-navbar--inverse': navbarLayoutType === 'inverse'
    }"
  >
    <nav class="site-navbar">
      <div class="site-navbar__header">
        <h1 class="site-navbar__brand" @click="$router.push({ name: 'home' })">
          <span class="site-navbar__brand-lg">自习室预约管理系统</span>
          <span class="site-navbar__brand-mini">自习室</span>
        </h1>
      </div>
      <div class="site-navbar__body">
        <el-menu class="site-navbar__menu site-navbar__menu--left" mode="horizontal">
          <el-menu-item
            class="site-navbar__switch"
            index="fold"
            @click="toggleSidebar"
          >
            <icon-svg name="menu" />
          </el-menu-item>
        </el-menu>
        <div class="site-navbar__actions">
          <el-menu class="site-navbar__menu site-navbar__menu--right" mode="horizontal">
            <el-menu-item class="site-navbar__avatar" index="user">
              <el-dropdown trigger="click" @command="handleCommand">
                <span class="el-dropdown-link">{{ userName }}</span>
                <el-dropdown-menu slot="dropdown">
                  <el-dropdown-item command="theme">布局设置</el-dropdown-item>
                  <el-dropdown-item command="logout" divided>退出登录</el-dropdown-item>
                </el-dropdown-menu>
              </el-dropdown>
            </el-menu-item>
          </el-menu>
          <el-button
            class="site-navbar__logout-btn"
            type="text"
            icon="el-icon-switch-button"
            @click="logout"
          >退出登录</el-button>
        </div>
      </div>
    </nav>

    <aside
      class="site-sidebar"
      :class="'site-sidebar--' + sidebarLayoutSkin"
    >
      <div class="site-sidebar__inner">
        <el-menu
          :default-active="menuActiveName"
          class="site-sidebar__menu"
          unique-opened
        >
          <!-- 后端菜单为空时仍有入口（/sys/menu/nav 当前返回空列表） -->
          <el-menu-item index="home" @click.native="$router.push({ name: 'home' })">
            <span>首页</span>
          </el-menu-item>
          <el-menu-item index="bas-analytics-revenue" @click.native="$router.push({ name: 'bas-analytics-revenue' })">
            <span>营收统计</span>
          </el-menu-item>
          <el-menu-item index="bas-analytics-util" @click.native="$router.push({ name: 'bas-analytics-util' })">
            <span>座位利用率</span>
          </el-menu-item>
          <el-menu-item index="bas-seat-editor" @click.native="$router.push({ name: 'bas-seat-editor' })">
            <span>座位编辑器</span>
          </el-menu-item>
          <el-menu-item index="bas-sys-log" @click.native="$router.push({ name: 'bas-sys-log' })">
            <span>操作日志</span>
          </el-menu-item>
          <el-menu-item index="theme" @click.native="$router.push({ name: 'theme' })">
            <span>布局设置</span>
          </el-menu-item>
          <main-sidebar-submenu
            v-for="item in menuList"
            :key="item.menuId"
            :menu="item"
            @nav="openMenuRoute"
          />
        </el-menu>
      </div>
    </aside>

    <div class="site-content__wrapper">
      <main class="site-content">
        <router-view />
      </main>
    </div>
  </div>
</template>

<script>
import { clearLoginInfo } from '@/utils'
import MainSidebarSubmenu from '@/components/main-sidebar-submenu.vue'

export default {
  name: 'MainLayout',
  components: {
    MainSidebarSubmenu
  },
  computed: {
    sidebarFold: {
      get () {
        return this.$store.state.common.sidebarFold
      },
      set (v) {
        this.$store.commit('common/updateSidebarFold', v)
      }
    },
    navbarLayoutType () {
      return this.$store.state.common.navbarLayoutType
    },
    sidebarLayoutSkin () {
      return this.$store.state.common.sidebarLayoutSkin
    },
    menuList () {
      return this.$store.state.common.menuList
    },
    menuActiveName () {
      return this.$store.state.common.menuActiveName
    },
    userName () {
      const n = this.$store.state.user.name
      return (n && String(n).trim()) || '管理员'
    }
  },
  watch: {
    $route: {
      handler (route) {
        if (route && route.name) {
          this.$store.commit('common/updateMenuActiveName', route.name)
        }
      },
      immediate: true
    }
  },
  created () {
    try {
      const raw = sessionStorage.getItem('menuList')
      const list = raw ? JSON.parse(raw) : []
      if (Array.isArray(list) && list.length) {
        this.$store.commit('common/updateMenuList', list)
      }
    } catch (e) {
      console.warn('menuList parse failed', e)
    }
  },
  methods: {
    toggleSidebar () {
      this.sidebarFold = !this.sidebarFold
    },
    handleCommand (cmd) {
      if (cmd === 'theme') {
        this.$router.push({ name: 'theme' })
      }
      if (cmd === 'logout') {
        this.logout()
      }
    },
    logout () {
      this.$confirm('确定退出当前账号？', '退出登录', {
        confirmButtonText: '退出',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(() => {
        clearLoginInfo()
        this.$router.push({ name: 'login' })
      }).catch(() => {})
    },
    openMenuRoute (menu) {
      if (!menu || !menu.url || !/\S/.test(menu.url)) return
      if (/^https?:\/\//i.test(menu.url)) {
        Promise.resolve(this.$router.push({ name: 'i-' + menu.menuId })).catch(() => {})
        return
      }
      const name = menu.url.replace(/\//g, '-')
      Promise.resolve(this.$router.push({ name })).catch(() => {})
    },

  }
}
</script>
