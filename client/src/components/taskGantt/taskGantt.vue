<template><GanttPanel title="任务甘特图" :rows="timeline.rows" :horizon="timeline.horizon" :tasks="tasks" :axis="axis" @zoom="$emit('zoom', $event)"/></template>
<script>
import GanttPanel from '../gantt/GanttPanel.vue'
export default { name: 'taskGantt', components: { GanttPanel }, props: { timeline: { type: Object, required: true }, tasks: { type: Array, default: () => [] }, axis: { type: Object, required: true } } }
</script>
