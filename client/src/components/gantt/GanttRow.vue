<template>
  <div class="row" :class="highlight" :data-task-id="row.id" :data-job-id="row.jobId" :style="{ width: axis.width + 'px', '--tick-width': axis.cellWidth + 'px' }" :title="details">
    <div class="mixed-source">
      <MixedTaskLines :event-informations="eventInformations"
                 :event-time-points="eventTimePoints" :time-axis-length="axis.columns" :axis-cell-width="axis.cellWidth" :criticality-switch-time="-1"/>
    </div>
  </div>
</template>
<script>
// Bundled Mixed renderer; preserve its geometry and SVG.
import MixedTaskSource from '../mixed/taskGanttChartLines.vue'
// Keep mixed template, duration(), symbolPosition() and SVG unchanged. Only configure grid density.
const axisAdapter = source => ({ extends: source, props: { axisCellWidth: Number }, computed: {
  columnNumbers() { return { ...source.computed.columnNumbers.call(this),
    'grid-template-columns': `repeat(${this.timeAxisLength}, ${this.axisCellWidth}px)` } }
} })
const MixedTaskLines = axisAdapter(MixedTaskSource)
export default {
  name: 'GanttRow', components: { MixedTaskLines },
  props: { row: { type: Object, required: true }, axis: { type: Object, required: true }, highlight: String },
  computed: {
    eventInformations() { return this.row.states.filter(s => ['executing','interference','queued','idle'].includes(s.state))
      .map(s => ({ staticPid: s.id == null ? -1 : s.id, dynamicPid: s.id,
        state: s.state === 'executing' ? 'normal-execution' : s.state === 'idle' ? 'spare' : s.state,
        startTime: s.start * this.axis.pixelsPerMs / 80, endTime: s.end * this.axis.pixelsPerMs / 80 })) },
    eventTimePoints() { return this.row.events.filter(e => ['release','completion'].includes(e.event))
      .map(e => ({ event: e.event, eventTime: e.time * this.axis.pixelsPerMs / 80, staticPid: e.id, dynamicPid: e.jobId })) },
    details() { return this.row.states.map(s => `${s.state} [${s.start}, ${s.end}) ms · ${s.id == null ? 'idle' : 'Task ' + s.id}`).join('\n') }
  }
}
</script>
<style scoped>
.row { position: relative; min-height: 44px; flex: 1; background: white; overflow: hidden; border-bottom: 2px solid rgb(208,211,212); box-sizing: border-box; }
.mixed-source { position: absolute; left: 20px; right: 0; top: calc(50% - 22px); transform: scaleY(.55); transform-origin: top left; }
.mixed-source ::v-deep .taskGanttChartLinesStyle { width: 100%; }
.mixed-source ::v-deep .chart-item { width: var(--tick-width); }
/* RTA annotation: geometry is calculated by mixed's duration(). */
.mixed-source ::v-deep .interference-block { position: absolute; top: 40px; height: 40px; border: 2px dashed black; box-sizing: border-box; background: white; }
.mixed-source ::v-deep .queued-block { position: absolute; top: 40px; height: 40px; border: 2px solid #626b73; box-sizing: border-box; background: repeating-linear-gradient(135deg, #edf0f2 0, #edf0f2 5px, #aab2ba 5px, #aab2ba 7px); }
.row.pass ::v-deep .normal-execution-block { background-color: #35a65e; }
.row.miss ::v-deep .normal-execution-block { background-color: #e35c5c; }
</style>
