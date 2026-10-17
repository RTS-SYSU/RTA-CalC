<template>
  <tr><td v-for="field in fields" :key="field">{{ taskInformation[field] == null ? '/' : taskInformation[field] }}</td><td :title="taskInformation.rtaNote">{{ taskInformation.unbounded ? '无界' : taskInformation.nonConvergent ? '未确定' : taskInformation.wcrt == null ? '/' : taskInformation.wcrt }}</td><td :class="taskInformation.schedulable === true ? 'pass' : taskInformation.schedulable === false ? 'miss' : ''" :title="taskInformation.rtaNote">{{ taskInformation.schedulable === true ? 'pass' : taskInformation.schedulable === false ? 'miss' : taskInformation.nonConvergent ? '未确定' : '/' }}</td></tr>
</template>
<script>
export default { name: 'TaskInformation', props: { taskInformation: { type: Object, required: true }, fields: { type: Array, required: true } } }
</script>
<style scoped>
td { border: 2px solid rgb(208,211,212); height: 28px; padding: 0 5px; white-space: nowrap; font-weight: bold; }
tr:hover { background: rgb(246,246,246); }
td.pass { color: #16783c; background: #e7f5eb; }
td.miss { color: #b92323; background: #fdeaea; }
small { font-weight: normal; font-size: 11px; }
</style>
