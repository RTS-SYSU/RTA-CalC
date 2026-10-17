// Visualization only. Never modifies milliseconds in the frozen simulation result.
const CPU_INFO_WIDTH = 110
const TASK_INFO_WIDTH = 512
function createTimeAxis(horizon, availableWidth, tickInterval) {
  const range = Number.isFinite(horizon) && horizon > 0 ? horizon : 100
  if ([1,10,100].includes(tickInterval)) {
    const cellWidth = Math.max(80, ((availableWidth || 600) - 40) * tickInterval / range)
    const pixelsPerMs = cellWidth / tickInterval
    const ticks = Array.from({ length: Math.floor(range / tickInterval) + 1 }, (_,i)=>i * tickInterval)
    if (ticks[ticks.length - 1] !== range) ticks.push(range)
    return { horizon: range, width: range * pixelsPerMs + 40, pixelsPerMs,
      interval: tickInterval, ticks, columns: Math.ceil(range / tickInterval) + 1, cellWidth }
  }
  const width = Math.max(240, availableWidth || 600)
  const usable = width - 40
  const desired = range / Math.max(1, Math.floor(usable / 70))
  const power = Math.pow(10, Math.floor(Math.log10(desired)))
  const interval = [1,2,5,10].find(n => n * power >= desired) * power
  const count = Math.floor(range / interval)
  const ticks = Array.from({ length: count + 1 }, (_,i)=> Number((i * interval).toPrecision(12)))
  return { horizon: range, width, pixelsPerMs: usable / range, interval, ticks,
    columns: Math.ceil(range / interval) + 1, cellWidth: interval * usable / range }
}
module.exports = { CPU_INFO_WIDTH, TASK_INFO_WIDTH, createTimeAxis }
