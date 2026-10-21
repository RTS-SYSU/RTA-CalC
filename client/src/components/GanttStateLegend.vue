<template>
  <div class="legend">
    <div><span class="box"></span><span>executing without locks</span></div>
    <div><svg width="50" height="40" viewBox="0 0 40 80"><line x1="20" y1="30" x2="20" y2="80" stroke="black" stroke-width="3"/><path d="M20 10 L10 30 L30 30Z" fill="black"/></svg><span>release a task</span></div>
    <div><svg width="50" height="40" viewBox="0 0 40 80"><line x1="20" y1="20" x2="20" y2="80" stroke="black" stroke-width="3"/><line x1="10" y1="20" x2="30" y2="20" stroke="black" stroke-width="3"/></svg><span>complete a task</span></div>
    <div><span class="box interference"></span><span>interference</span></div>
    <div><span class="box queued"></span><span>queued（等待同一任务的前序作业）</span></div>
  </div>
</template>
<script>export default { name: 'GanttStateLegend' }</script>
<style scoped>
.legend { border: 2px solid rgb(208,211,212); padding: 5px; flex: none; color: #626b73; font-weight: bold; text-align: left; font-size: 12px; }
.legend > div { display: flex; align-items: center; gap: 6px; min-height: 36px; }
.box { box-sizing: border-box; display: inline-block; width: 50px; height: 25px; border: 2px solid black; }
.interference { border-style: dashed; }
.queued { border-color: #626b73; background: repeating-linear-gradient(135deg, #edf0f2 0, #edf0f2 5px, #aab2ba 5px, #aab2ba 7px); }
</style>
