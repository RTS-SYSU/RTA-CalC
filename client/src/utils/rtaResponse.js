// API boundary only: join task analysis by identity and freeze the completed timeline.
// Pagination never calls this module or performs scheduling.
function number(value) {
  if (value === null || value === undefined || value === '') return null
  const parsed = Number(value)
  return Number.isFinite(parsed) ? parsed : null
}
function boolean(value) {
  if (value === true || value === 'true') return true
  if (value === false || value === 'false') return false
  return null
}
function freeze(value) {
  if (value && typeof value === 'object' && !Object.isFrozen(value)) {
    Object.values(value).forEach(freeze)
    Object.freeze(value)
  }
  return value
}
function normalizeResponse(data, input) {
  const sources = Array.isArray(data.systemTasks) ? data.systemTasks : input.tasks
  if (!Array.isArray(sources)) throw new Error('接口缺少任务配置数据')
  const results = Array.isArray(data.tasks) ? data.tasks : []
  const systemTasks = sources.map(source => {
    const matches = results.filter(r => r.id != null && String(r.id) === String(source.id))
    if (matches.length > 1) throw new Error('任务 RTA 结果无法唯一匹配：' + source.id)
    const result = matches[0] || {}
    const wcrt = number(source.wcrt == null ? result.responseTimeMs : source.wcrt)
    const deadline = number(source.deadline == null ? result.deadlineMs : source.deadline)
    const starved = boolean(source.starved == null ? result.starved : source.starved) === true
    const converged = boolean(source.converged == null ? result.converged : source.converged)
    const nonConvergent = boolean(source.nonConvergent) === true || starved || converged === false
    // Finite WCRT is compared without display rounding. Unbounded R is always a miss.
    const scheduled = starved || wcrt !== null && deadline !== null && wcrt > deadline ? false
      : converged === false ? boolean(source.schedulable == null ? result.schedulable : source.schedulable)
        : wcrt !== null && deadline !== null ? wcrt <= deadline : boolean(source.schedulable == null ? result.schedulable : source.schedulable)
    return freeze({ ...source, wcrt, deadline, starved, converged, nonConvergent, schedulable: scheduled,
      rtaNote: source.rtaNote || result.note || (scheduled === null ? '未获得 RTA 结果' : '按 WCRT <= deadline 判断') })
  })
  const sourceTimeline = data.timeline
  if (!sourceTimeline || !Array.isArray(sourceTimeline.rows)) throw new Error('接口缺少完整甘特图结果')
  const seen = new Set()
  const rows = sourceTimeline.rows.map(row => {
    const task = systemTasks.find(t => String(t.id) === String(row.id))
    if (!task) throw new Error('甘特图任务缺少对应输入：' + row.id)
    const phase = number(task.phase), period = number(task.period), release = number(row.release)
    if (phase === null || period === null || period <= 0 || release === null) throw new Error('作业释放参数无效')
    const jobIndex = row.jobIndex == null ? Math.round((release - phase) / period) : number(row.jobIndex)
    if (!Number.isInteger(jobIndex) || jobIndex < 0 || Math.abs(release - (phase + jobIndex * period)) > 1e-7)
      throw new Error('作业 release 与 phase + jobIndex × period 不一致')
    const jobId = String(task.id) + ':' + jobIndex
    if ((row.jobId != null && row.jobId !== jobId) || seen.has(jobId)) throw new Error('作业 ID 不稳定或重复：' + jobId)
    seen.add(jobId)
    const states = (row.states || []).map(s => ({ ...s }))
    for (const s of states) {
      const owner = s.taskId == null ? s.id : s.taskId
      if (s.taskId != null && String(s.taskId) !== String(s.id)) throw new Error('区间 Task ID 不一致')
      if (s.state === 'executing' && (String(owner) !== String(task.id) || s.jobId !== jobId))
        throw new Error('执行区间不属于当前 Task ID / job')
      s.taskId = owner
    }
    const events = (row.events || []).map(e => ({ ...e }))
    const completionEvent = events.find(e => e.event === 'completion')
    return { ...row, jobIndex, jobId, release, interference: number(row.interference),
      completion: row.completion == null ? completionEvent ? number(completionEvent.time) : null : number(row.completion),
      observationEnd: number(row.observationEnd) || (completionEvent ? number(completionEvent.time) : number(sourceTimeline.horizon)),
      states, events }
  })
  return { systemTasks, timeline: freeze({ ...sourceTimeline, rows,
    cpu: (sourceTimeline.cpu || []).map(s => ({ ...s })), events: (sourceTimeline.events || []).map(e => ({ ...e })) }),
  warnings: data.warnings || [], analysis: data.analysis }
}
module.exports = { normalizeResponse }
