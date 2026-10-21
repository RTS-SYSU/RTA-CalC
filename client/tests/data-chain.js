/* global BigInt */
const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const vm = require('node:vm')
const Vue = require('vue')
const compiler = require('vue-template-compiler')
const { normalizeResponse } = require('../src/utils/rtaResponse')
const { createTimeAxis, TASK_INFO_WIDTH } = require('../src/utils/timeAxis')
const input = JSON.parse(fs.readFileSync(path.join(__dirname, '../../server/src/test/resources/fixtures/tasks.json'), 'utf8'))
const response = JSON.parse(fs.readFileSync(path.join(__dirname, '../../server/target/rta-response-test.json'), 'utf8'))
const result = normalizeResponse(response, input)
assert.deepEqual(result.systemTasks.map(t => t.schedulable), [true,true,true,false,false,false])
assert(Object.isFrozen(result.timeline.rows))
function loadComponent(file, imports = {}) {
  const parsed = compiler.parseComponent(fs.readFileSync(file, 'utf8'))
  const source = parsed.script.content.replace(/^import .*$/gm, '').replace('export default', 'module.exports =')
  const box = { module: { exports: {} }, GanttRow: { render(h) { return h('div') } }, process,
    require: name=>require(path.resolve(path.dirname(file),name)), ...imports }
  vm.runInNewContext(source, box)
  return { ...box.module.exports, ...compiler.compileToFunctions(parsed.template.content) }
}
const panel = new Vue({ ...loadComponent(path.join(__dirname, '../src/components/gantt/GanttPanel.vue')),
  propsData: { rows: result.timeline.rows, horizon: result.timeline.horizon, cpu: false, title: '任务甘特图', tasks: result.systemTasks, axis:createTimeAxis(result.timeline.horizon,760) } })
const tableRow = loadComponent(path.join(__dirname, '../src/components/taskDetails/TaskInformation.vue'))
function texts(vnode) { return vnode.text || (vnode.children || []).map(texts).join('') }
for (const task of result.systemTasks) {
  const row = new Vue({ ...tableRow, propsData: { taskInformation: task, fields: ['id','name','priority','period','phase','deadline','WCET'] } })
  assert(texts(row._render()).includes(task.schedulable ? 'pass' : 'miss'))
  assert(!texts(row._render()).includes('∞'))
  assert(!texts(row._render()).includes('non-convergent'))
  const ticks = v => BigInt(Math.round(v * 10))
  let responseTicks = ticks(task.WCET)
  for (const round of task.rtaDetails.trace) {
    assert.equal(ticks(round.response),responseTicks)
    let next = ticks(task.WCET)
    for (const h of round.contributions) {
      const hp = input.tasks.find(t=>t.id===h.id)
      assert(hp.priority < task.priority)
      const count = (responseTicks + ticks(hp.period) - 1n) / ticks(hp.period)
      assert.equal(BigInt(h.count), count)
      assert.equal(ticks(h.interference), count * ticks(hp.WCET))
      next += count * ticks(hp.WCET)
    }
    assert.equal(ticks(round.next),next)
    responseTicks = next
  }
}
// Load the actual mixed SFCs: core geometry and selected renderer must be the originals.
const mixedRoot = path.join(__dirname,'../src/components/mixed')
const MixedTaskLines = loadComponent(path.join(mixedRoot,'taskGanttChartLines.vue'))
const rowComponent = loadComponent(path.join(__dirname,'../src/components/gantt/GanttRow.vue'),{ MixedTaskSource:MixedTaskLines })
for (const width of [400,760,1280]) {
  const row = result.timeline.rows[3]
  const axis=createTimeAxis(result.timeline.horizon,width)
  const wrapper = new Vue({ ...rowComponent, propsData: { row, axis, highlight:'pass' } })
  const source=wrapper.$options.components.MixedTaskLines
  assert.strictEqual(source.extends,MixedTaskLines)
  const renderer = new Vue({ ...source, propsData: {
    eventInformations: wrapper.eventInformations,eventTimePoints:wrapper.eventTimePoints,timeAxisLength:axis.columns,axisCellWidth:axis.cellWidth,criticalitySwitchTime:-1
  } })
  wrapper.eventInformations.forEach((e,i)=> {
    const actual=row.states.filter(s=>['executing','interference','queued','idle'].includes(s.state))[i]
    assert(Math.abs(parseFloat(renderer.duration(e).width)-(actual.end-actual.start)*axis.pixelsPerMs)<1e-8)
    assert(Math.abs(parseFloat(renderer.duration(e).left)-actual.start*axis.pixelsPerMs)<1e-8)
  })
  assert(renderer.columnNumbers['grid-template-columns'].includes(axis.cellWidth+'px'))
  assert(renderer._render())
}
// Missing status is joined strictly by id, never by a possibly duplicated task name.
const legacy = JSON.parse(JSON.stringify(response))
legacy.systemTasks.forEach(t => { delete t.schedulable; delete t.rtaNote })
legacy.timeline.rows.forEach(r => { delete r.jobId; delete r.jobIndex; delete r.completion; delete r.observationEnd })
assert.deepEqual(normalizeResponse(legacy,input).systemTasks.map(t=>t.schedulable),[true,true,true,false,false,false])
const rawOnly = { ...legacy, systemTasks: input.tasks }
assert.deepEqual(normalizeResponse(rawOnly,input).systemTasks.map(t=>t.schedulable),[true,true,true,false,false,false])
const missingIds=JSON.parse(JSON.stringify(rawOnly))
missingIds.tasks.forEach(t=>{delete t.id})
assert(normalizeResponse(missingIds,input).systemTasks.every(t=>t.wcrt===null&&t.schedulable===null))
const stringNumbers = JSON.parse(JSON.stringify(response))
stringNumbers.systemTasks.forEach(t => { if(t.wcrt!=null)t.wcrt=String(t.wcrt);t.deadline=String(t.deadline) })
assert.deepEqual(normalizeResponse(stringNumbers,input).systemTasks.map(t=>t.schedulable),[true,true,true,false,false,false])
const before = JSON.stringify(result.timeline)
for (let i=0;i<100;i++) {
  assert.equal(JSON.stringify(panel.rows),JSON.stringify(result.timeline.rows))
  const rendered=panel._render()
  const allKeys=[]
  const visit = v => {
    if(v.key!=null)allKeys.push(v.key)
    if(v.data && (v.data.staticClass==='info-row'||v.data.staticClass==='heading'))
      assert.equal((v.children||[]).filter(c=>c.tag==='span').length,5)
    ;(v.children||[]).forEach(visit)
  }
  visit(rendered)
  panel.rows.forEach(row=>assert(allKeys.includes(row.jobId)))
  assert(!texts(rendered).includes('1 / 2'))
  assert.equal(panel.rows.length,102)
  assert(!texts(rendered).includes('job id'))
  assert(!texts(rendered).toLowerCase().includes('partition'))
  panel.rows.forEach(row=> {
    assert(!texts(rendered).includes(row.jobId))
    assert.strictEqual(panel.task(row),result.systemTasks.find(t=>t.id===row.id))
    assert.equal(panel.status(row),panel.task(row).schedulable?'pass':'miss')
  })
}
assert.equal(JSON.stringify(result.timeline),before)
// Clicking ONLY toggles visualization state, never imports, analyzes, or rewrites a timeline.
const parentComponent=loadComponent(path.join(__dirname,'../src/components/GanttComponent.vue'),{
  GanttStateLegend:{},TaskTable:{},taskGantt:{}
})
const parent=new Vue(parentComponent)
parent.timeline=result.timeline;parent.systemTasks=result.systemTasks
parent.$refs.layout={clientWidth:1366}
parent.measureAxis()
assert.equal(parent.plotWidth,1366-24-4-TASK_INFO_WIDTH)
assert.equal(parent.axis.width,8040)
assert.equal(parent.axis.horizon,1000)
assert.equal(parent.axis.pixelsPerMs*1000+40,parent.axis.width)
assert.equal(parent.axis.interval,10)
const zoomTimeline=JSON.stringify(parent.timeline)
const zoomTasks=JSON.stringify(parent.systemTasks)
parent.zoomAxis(-1);assert.equal(parent.axis.interval,1)
assert.equal(parent.axis.pixelsPerMs,80)
parent.zoomAxis(-1);assert.equal(parent.axis.interval,1)
parent.zoomAxis(1);assert.equal(parent.axis.interval,10)
parent.zoomAxis(1);assert.equal(parent.axis.interval,100)
parent.zoomAxis(1);assert.equal(parent.axis.interval,100)
assert.equal(parent.axis.horizon,1000)
assert.equal(JSON.stringify(parent.timeline),zoomTimeline)
assert.equal(JSON.stringify(parent.systemTasks),zoomTasks)
panel.axis=parent.axis
const wheelEvents=[];let prevented=0
panel.$on('zoom',direction=>wheelEvents.push(direction))
panel.wheelZoom({deltaY:-100,deltaX:0,preventDefault(){prevented++}})
assert.deepEqual(wheelEvents,[-1]);assert.equal(prevented,1)
// Every row is colored immediately using its own Task ID analysis.
function executionHighlights(vnode) {
  const found=[]
  function visit(v) {
    const props=v.componentOptions && v.componentOptions.propsData || {}
    const attrs=v.data && v.data.attrs || {}
    if(props.highlight || attrs.highlight) found.push({jobId:(props.row || attrs.row).jobId,status:props.highlight || attrs.highlight})
    ;(v.children||[]).forEach(visit)
  }
  visit(vnode)
  return found
}
const automatic=executionHighlights(panel._render())
assert.equal(automatic.length,102)
for(const row of panel.rows)assert.equal(automatic.find(h=>h.jobId===row.jobId).status,panel.task(row).schedulable?'pass':'miss')
assert.equal(parent.selectJob,undefined)
assert(!texts(parent._render()).includes('CPU'))
assert.equal(JSON.stringify(parent.timeline),before)
assert(!fs.existsSync(path.join(__dirname,'../src/components/RtaDetails.vue')))
assert.equal(result.systemTasks[4].wcrt,null);assert.equal(result.systemTasks[5].wcrt,null)
assert.equal(result.systemTasks[4].lastIterationValue,227.7);assert.equal(result.systemTasks[5].lastIterationValue,1217.3)
for(const horizon of [.3,33,100,1000])for(const width of [400,760,1280]){
  const axis=createTimeAxis(horizon,width)
  assert(axis.ticks.length<=Math.floor((width-40)/70)+2)
  assert(axis.pixelsPerMs*horizon+40<=width+1e-8)
  assert(axis.ticks.every(t=>t>=0&&t<=horizon))
}
const invalid = JSON.parse(JSON.stringify(response))
invalid.timeline.rows[7].release=0
assert.throws(()=>normalizeResponse(invalid,input),/不一致/)
const duplicateInput=JSON.parse(fs.readFileSync(path.join(__dirname,'../../server/src/test/resources/fixtures/duplicate-task-names.json'),'utf8'))
const duplicateResponse=JSON.parse(fs.readFileSync(path.join(__dirname,'../../server/target/rta-duplicate-names-test.json'),'utf8'))
const duplicate=normalizeResponse(duplicateResponse,duplicateInput)
const dupeParent=new Vue(parentComponent);dupeParent.timeline=duplicate.timeline;dupeParent.systemTasks=duplicate.systemTasks
const dupePanel=new Vue({ ...loadComponent(path.join(__dirname,'../src/components/gantt/GanttPanel.vue')),
  propsData:{rows:duplicate.timeline.rows,tasks:duplicate.systemTasks,axis:createTimeAxis(100,826)}
})
const dupeBefore=JSON.stringify(duplicate)
assert.deepEqual(executionHighlights(dupePanel._render()),[{jobId:'4:0',status:'miss'},{jobId:'5:0',status:'pass'}])
assert.equal(dupePanel.status(duplicate.timeline.rows[0]),'miss')
assert.equal(dupePanel.status(duplicate.timeline.rows[1]),'pass')
assert.equal(JSON.stringify(duplicate),dupeBefore)
const badOwner=JSON.parse(JSON.stringify(duplicateResponse))
badOwner.timeline.rows[0].states.find(s=>s.state==='executing').taskId=5
assert.throws(()=>normalizeResponse(badOwner,duplicateInput),/Task ID/)
function fixture(name) {
  const data=JSON.parse(fs.readFileSync(path.join(__dirname,'../../server/target/'+name),'utf8'))
  return normalizeResponse(data,{tasks:data.systemTasks})
}
const arbitrary=fixture('rta-arbitrary-deadline-test.json')
assert.equal(arbitrary.systemTasks[1].wcrt,118)
assert.equal(arbitrary.systemTasks[1].schedulable,false)
assert.equal(arbitrary.analysis.schedulable,false)
// The second low-priority job waits for its predecessor from 100 to 114 ms.
// Render that waiting time even when the observation window ends before completion.
const queuedJob=arbitrary.timeline.rows.find(r=>r.jobId==='2:1')
assert.equal(queuedJob.queued,14)
assert.equal(queuedJob.completion,null)
for(const interval of [1,10,100]) {
  const axis=createTimeAxis(arbitrary.timeline.horizon,760,interval)
  const wrapper=new Vue({...rowComponent,propsData:{row:queuedJob,axis,highlight:'miss'}})
  const renderer=new Vue({...wrapper.$options.components.MixedTaskLines,propsData:{
    eventInformations:wrapper.eventInformations,eventTimePoints:wrapper.eventTimePoints,
    timeAxisLength:axis.columns,axisCellWidth:axis.cellWidth,criticalitySwitchTime:-1
  }})
  const blocks=[]
  const collect = vnode => {
    if(vnode.data && vnode.data.class==='queued-block')blocks.push(vnode)
    ;(vnode.children || []).forEach(collect)
  }
  collect(renderer._render())
  assert.equal(blocks.length,1)
  assert(Math.abs(parseFloat(blocks[0].data.style.left)-100*axis.pixelsPerMs)<1e-8)
  assert(Math.abs(parseFloat(blocks[0].data.style.width)-14*axis.pixelsPerMs)<1e-8)
  const drawnMs=wrapper.eventInformations.reduce((sum,e)=>sum+(e.endTime-e.startTime)*80/axis.pixelsPerMs,0)
  assert(Math.abs(drawnMs-(queuedJob.observationEnd-queuedJob.release))<1e-8)
}
const legend=new Vue(loadComponent(path.join(__dirname,'../src/components/GanttStateLegend.vue')))
assert(texts(legend._render()).includes('queued（等待同一任务的前序作业）'))
const skipped=fixture('rta-skipped-test.json')
for(const task of skipped.systemTasks) {
  assert.equal(task.nonConvergent,false)
  assert.equal(task.schedulable,null)
  const row=new Vue({...tableRow,propsData:{taskInformation:task,fields:['id','WCET']}})
  assert(!texts(row._render()).includes('末值'))
  assert(!texts(row._render()).includes('未确定'))
}
const skippedPage=new Vue(parentComponent)
skippedPage.systemTasks=skipped.systemTasks
assert(!texts(skippedPage._render()).includes('分析上限'))
for(const task of result.systemTasks.slice(3)) {
  assert.equal(task.unbounded,true)
  assert.equal(task.wcrt,null)
  const row=new Vue({...tableRow,propsData:{taskInformation:task,fields:['id']}})
  assert(texts(row._render()).includes('无界'))
  assert(texts(row._render()).includes('miss'))
}
const unknownTask={id:1,WCET:1,period:2,deadline:10,converged:false,nonConvergent:true,
  wcrt:null,lastIterationValue:3,schedulable:null}
const unknown=normalizeResponse({systemTasks:[unknownTask],timeline:{rows:[]},analysis:{schedulable:null,totalUtilization:.5}}, {tasks:[unknownTask]})
assert.equal(unknown.systemTasks[0].wcrt,null)
assert.equal(unknown.systemTasks[0].schedulable,null)
const unknownRow=new Vue({...tableRow,propsData:{taskInformation:unknown.systemTasks[0],fields:['id']}})
assert(texts(unknownRow._render()).includes('未确定'))
const unknownPage=new Vue(parentComponent)
unknownPage.analysis=unknown.analysis;unknownPage.systemTasks=unknown.systemTasks
assert(texts(unknownPage._render()).includes('可调度性未确定'))
const precise=fixture('rta-small-times-test.json')
const precisePanel=new Vue({...loadComponent(path.join(__dirname,'../src/components/gantt/GanttPanel.vue')),
  propsData:{rows:precise.timeline.rows,tasks:precise.systemTasks,axis:createTimeAxis(.001,760),horizon:.001}})
const preciseText=texts(precisePanel._render())
assert(preciseText.includes('0.0001'))
assert(preciseText.includes('0.0005'))
for(const value of [0,.0004,.000000001,1.234567891])assert.equal(Number(precisePanel.format(value)),value)
assert.equal(precisePanel.format(null),'/')
console.log('PASS: arbitrary-deadline WCRT, unbounded/unknown/skipped states, precise times, geometry, 102 jobs, immutable zooms and Task ID colors')
