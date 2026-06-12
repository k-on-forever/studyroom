<template>
  <div>
    <el-submenu
      v-if="menu.list && menu.list.length"
      :index="'sub-' + menu.menuId"
    >
      <template slot="title">
        <icon-svg
          v-if="menu.icon"
          :name="menu.icon"
          class="site-sidebar__menu-icon"
        />
        <span>{{ menu.name }}</span>
      </template>
      <main-sidebar-submenu
        v-for="child in menu.list"
        :key="child.menuId"
        :menu="child"
        @nav="$emit('nav', $event)"
      />
    </el-submenu>
    <el-menu-item
      v-else-if="menu.url && /\S/.test(menu.url)"
      :index="menuIndex(menu)"
      @click.native="$emit('nav', menu)"
    >
      <icon-svg
        v-if="menu.icon"
        :name="menu.icon"
        class="site-sidebar__menu-icon"
      />
      <span>{{ menu.name }}</span>
    </el-menu-item>
  </div>
</template>

<script>
export default {
  name: 'MainSidebarSubmenu',
  props: {
    menu: {
      type: Object,
      required: true
    }
  },
  methods: {
    menuIndex (menu) {
      if (/^https?:\/\//i.test(menu.url)) {
        return 'i-' + menu.menuId
      }
      return menu.url.replace(/\//g, '-')
    }
  }
}
</script>
