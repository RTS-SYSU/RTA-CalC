<template>
  <GanttComponent/>
</template>

<script>
import GanttComponent from './components/GanttComponent.vue'

export default {
  name: 'App',
  components: {
    GanttComponent : GanttComponent
  },
}
</script>

<style>
/* 整页按窗口宽度缩放，宽度正好等于窗口宽度，去掉多余的边距和横向滚动条 */
html, body {
  margin: 0;
  padding: 0;
  overflow-x: hidden;
}

#app {
  font-family: Avenir, Helvetica, Arial, sans-serif;
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
  text-align: center;
  color: #2c3e50;
}
</style>
