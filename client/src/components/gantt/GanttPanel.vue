<template>
  <section data-gantt="tasks">
    <h3>{{ title }} <small>0–{{ format(axis.horizon) }} ms · 刻度 {{ format(axis.interval) }} ms<span> · 时间轴滚轮缩放，左侧信息区滚轮上下滚动</span></small></h3>
    <div class="frame">
      <div class="information">
        <div class="heading"><template><span>id</span><span>name</span><span>release (ms)</span><span>interference (ms)</span><span>complete (ms)</span></template></div>
        <div v-for="row in rows" :key="rowKey(row)" class="info-row" :data-task-id="row.id" :data-job-id="row.jobId">
          <template><span>{{ row.id }}</span><span>{{ row.name }}</span><span>{{ format(row.release) }}</span><span :title="interferenceDetails(row)">{{ format(row.interference) }}</span><span :title="row.completion == null ? '当前窗口截止 ' + format(horizon) + ' ms，未完成' : ''">{{ row.completion == null ? '未完成' : format(row.completion) }}</span></template>
        </div>
      </div>
      <div class="plot" :style="{ width: width + 'px' }" @wheel="wheelZoom">
        <div class="axis" :style="{ width: width + 'px' }">
          <span v-for="tick in ticks" :key="tick" :style="{ left: position(tick) + 'px' }">{{ format(tick) }}</span>
        </div>
        <GanttRow v-for="row in rows" :key="rowKey(row)" :row="row" :axis="axis" :highlight="status(row)"/>
      </div>
    </div>
    <p v-if="!rows.length" class="empty">导入 JSON 后显示任务时间轴。</p>
  </section>
</template>
<script>
import GanttRow from './GanttRow.vue'
export default {
  name: 'GanttPanel', components: { GanttRow },
  props: { title: String, rows: { type: Array, default: () => [] }, horizon: { type: Number, default: 100 }, tasks: { type: Array, default: () => [] }, axis: { type: Object, required: true } },
  computed: {
    width() { return this.axis.width },
    ticks() { return this.axis.ticks }
  },
  methods: {
    wheelZoom(event) {
      if (!event.deltaY || Math.abs(event.deltaX)>Math.abs(event.deltaY)) return
      event.preventDefault()
      if (this.lastZoomAt && Date.now()-this.lastZoomAt<120) return
      this.lastZoomAt=Date.now()
      this.$emit('zoom',event.deltaY>0?1:-1)
    },
    rowKey(row) { return row.jobId },
    task(row) { return this.tasks.find(t => String(t.id) === String(row.id)) },
    status(row) { const t = this.task(row); return t && t.schedulable === true ? 'pass' : t && t.schedulable === false ? 'miss' : '/' },
    position(time) { return 20 + time * this.axis.pixelsPerMs },
    format(value) { return Number(Number(value).toFixed(3)) },
    interferenceDetails(row) { return row.states.filter(s => s.state === 'interference').map(s => '[' + this.format(s.start) + ', ' + this.format(s.end) + ') ms · id ' + s.id).join('\n') || '0 ms' }
  }
}
</script>
<style scoped>
section { margin-top: 8px; }
section[data-gantt="tasks"] { flex: 1; display: flex; flex-direction: column; }
section[data-gantt="tasks"] .frame { max-height: calc(100vh - 230px); }
h3 { font-size: 15px; text-align: left; color: #626b73; margin: 4px 0; }
h3 small { font-size: 12px; font-weight: normal; margin-left: 8px; }
.frame { display: flex; align-items: flex-start; max-height: 180px; overflow: auto; border: 2px solid rgb(208,211,212); }
.information { width: 512px; flex: none; display: flex; flex-direction: column; position: sticky; left: 0; z-index: 3; background: white; }
.heading,.info-row { display: grid; grid-template-columns: 36px 130px 100px 126px 120px; }
.heading { height: 30px; flex: none; }
.info-row { height: 44px; flex: none; }
.heading span,.info-row span { border-right: 2px solid rgb(208,211,212); border-bottom: 2px solid rgb(208,211,212); display: flex; align-items: center; justify-content: center; font-size: 13px; font-weight: bold; color: #626b73; overflow: hidden; box-sizing: border-box; }
.plot { flex: none; min-width: 0; overflow: hidden; display: flex; flex-direction: column; }
.plot ::v-deep .row { height: 44px; flex: none; }
.axis { position: relative; height: 30px; flex: none; background: white; border-bottom: 2px solid rgb(208,211,212); box-sizing: border-box; }
.axis span { position: absolute; height: 30px; line-height: 30px; transform: translateX(-50%); color: #626b73; font-size: 14px; font-weight: bold; }
.empty { color: #626b73; text-align: left; }
</style>
