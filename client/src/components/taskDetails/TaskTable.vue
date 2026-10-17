<template>
  <div class="task-table">
    <table>
      <thead><tr><th v-for="field in fields" :key="field">{{ field }}</th><th>WCRT (ms)</th><th>pass/miss</th></tr></thead>
      <tbody><TaskInformation v-for="task in visible" :key="task.id" :task-information="task" :fields="fields"/></tbody>
    </table>
    <div v-if="pages > 1" class="pages"><button :disabled="page === 0" @click="page--">&lt;</button> {{ page + 1 }} / {{ pages }} <button :disabled="page + 1 >= pages" @click="page++">&gt;</button></div>
  </div>
</template>
<script>
import TaskInformation from './TaskInformation.vue'
export default {
  name: 'TaskTable', components: { TaskInformation },
  props: { taskInformations: { type: Array, default: () => [] } },
  data() { return { page: 0, fields: ['id','name','priority','period','phase','deadline','WCET'] } },
  computed: { pages() { return Math.ceil(this.taskInformations.length / 7) }, visible() { return this.taskInformations.slice(this.page * 7, (this.page + 1) * 7) } },
  watch: { taskInformations() { this.page = 0 } }
}
</script>
<style scoped>
.task-table { flex: 1; min-width: 0; overflow-x: auto; }
table { width: 100%; border-collapse: collapse; font-size: 13px; color: #626b73; }
th { border: 2px solid rgb(208,211,212); height: 28px; padding: 0 5px; }
.pages { margin-top: 8px; }
button { background: white; border: 0; cursor: pointer; font-size: 18px; }
</style>
