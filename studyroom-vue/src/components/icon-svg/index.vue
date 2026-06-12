<template>
  <svg
    class="icon-svg"
    aria-hidden="true"
    xmlns="http://www.w3.org/2000/svg"
    xmlns:xlink="http://www.w3.org/1999/xlink"
  >
    <use :xlink:href="iconRef" />
  </svg>
</template>

<script>
/**
 * SVG Sprite 图标（配合 svg-sprite-loader + src/icons/svg）
 * 用法：<icon-svg name="admin" /> 对应 icon-admin.svg
 */
export default {
  name: 'IconSvg',
  props: {
    name: {
      type: String,
      required: true
    }
  },
  computed: {
    iconRef () {
      return '#icon-' + this.name
    }
  }
}
</script>

<style scoped>
.icon-svg {
  width: 1em;
  height: 1em;
  vertical-align: -0.15em;
  fill: currentColor;
  overflow: hidden;
}
</style>
