<template>
  <div class="total-layout" ref="layout">
    <div class="top-row">
      <div class="controls">
        <button class="import" :disabled="loading" @click="$refs.file.click()">{{ loading ? '分析中…' : '导入 json' }}</button>
        <input ref="file" type="file" accept=".json,application/json" hidden @change="importJson">
        <p v-if="fileName">{{ fileName }}</p>
        <p v-if="error" role="alert" class="error">{{ error }}</p>
        <p v-for="(warning, i) in warnings" :key="i">{{ warning }}</p>
        <p v-if="analysis">利用率 {{ (analysis.totalUtilization * 100).toFixed(2) }}% · {{ analysis.schedulable ? '可调度' : '不可调度' }}</p>
        <p v-if="systemTasks.some(t => t.nonConvergent)">未收敛任务显示最后实际迭代值，不是收敛 WCRT。</p>
      </div>
      <TaskTable :task-informations="systemTasks"/>
      <GanttStateLegend/>
    </div>
    <taskGantt :timeline="timeline" :tasks="systemTasks" :axis="axis" @zoom="zoomAxis"/>
  </div>
</template>
<script>
import GanttStateLegend from './GanttStateLegend.vue'
import TaskTable from './taskDetails/TaskTable.vue'
import taskGantt from './taskGantt/taskGantt.vue'
const { createTimeAxis, TASK_INFO_WIDTH } = require('../utils/timeAxis')
const { normalizeResponse } = require('../utils/rtaResponse')
const RTA_API = process.env.VUE_APP_RTA_API || 'http://localhost:8080/api/rta'
export default {
  name: 'GanttComponent',
  components: { GanttStateLegend, TaskTable, taskGantt },
  data() { return { tickInterval: 10, plotWidth: 600, timeline: { horizon: 100, cpu: [], events: [], rows: [] }, systemTasks: [], warnings: [], analysis: null, error: '', loading: false, fileName: '' } },
  computed: {
    axis() {
      const deadlines = this.systemTasks.map(t=>Number(t.deadline)).filter(d=>Number.isFinite(d) && d>=0)
      return createTimeAxis(deadlines.length ? Math.max(...deadlines) : 100, this.plotWidth, this.tickInterval)
    }
  },
  mounted() {
    this.measureAxis()
    window.addEventListener('resize', this.measureAxis)
    if (typeof ResizeObserver !== 'undefined') { this.axisObserver = new ResizeObserver(this.measureAxis); this.axisObserver.observe(this.$refs.layout) }
  },
  beforeDestroy() { window.removeEventListener('resize', this.measureAxis); if (this.axisObserver) this.axisObserver.disconnect() },
  methods: {
    zoomAxis(direction) {
      const levels=[1,10,100]
      this.tickInterval=levels[Math.max(0,Math.min(2,levels.indexOf(this.tickInterval)+direction))]
    },
    // Each plot fills its available width; both retain the same real time range.
    measureAxis() { this.plotWidth = Math.max(240, this.$refs.layout.clientWidth - 24 - 4 - TASK_INFO_WIDTH) },
    async importJson(event) {
      const file = event.target.files[0]
      event.target.value = ''
      if (!file) return
      this.loading = true
      this.error = ''
      try {
        const text = await file.text()
        const input = JSON.parse(text.replace(/^\uFEFF/, ''))
        const response = await this.$axios.post(RTA_API, text, { timeout: 30000, headers: { 'Content-Type': 'application/json;charset=UTF-8' } })
        const result = normalizeResponse(response.data, input)
        this.systemTasks = result.systemTasks
        this.tickInterval = 10
        this.timeline = result.timeline
        this.warnings = result.warnings
        this.analysis = result.analysis
        this.fileName = file.name
      } catch (error) {
        this.error = (error.response && error.response.data && error.response.data.message) || error.message || 'JSON 解析或分析失败，请检查输入文件。'
      } finally { this.loading = false }
    }
  }
}
</script>
<style scoped>
.total-layout { padding: 8px 12px; min-width: 1000px; min-height: 100vh; box-sizing: border-box; display: flex; flex-direction: column; }
.total-layout > * { flex-shrink: 0; }
.top-row { display: flex; gap: 10px; align-items: flex-start; }
.controls { width: 130px; flex: none; text-align: left; font-size: 12px; }
.controls p { margin: 6px 0; }
.import { background: rgb(95,175,255); color: white; border: 2px solid rgb(95,175,255); border-radius: 8px; padding: 8px 14px; font-size: 15px; cursor: pointer; }
.import:hover { background: white; color: rgb(95,175,255); }
.error { color: #b42318; }
</style>
