<template>
  <div class="stats-panel" v-loading="loading">
    <div v-if="!loading && error" class="stats-error">
      <el-empty description="统计数据加载失败" :image-size="72">
        <el-button type="primary" plain @click="load">重新加载</el-button>
        <p class="error-detail">{{ error }}</p>
      </el-empty>
    </div>

    <template v-else>
      <div class="metric-grid">
        <div class="metric-card">
          <div class="metric-icon blue"><el-icon><Document /></el-icon></div>
          <div class="metric-copy">
            <span class="metric-label">信息总量</span>
            <strong class="metric-value">{{ formatCount(overview.total) }}</strong>
            <span class="metric-foot">覆盖 {{ (overview.sources || []).length }} 个平台</span>
          </div>
        </div>
        <div class="metric-card">
          <div class="metric-icon red"><el-icon><Warning /></el-icon></div>
          <div class="metric-copy">
            <span class="metric-label">负面信息</span>
            <strong class="metric-value">{{ formatCount(negativeCount) }}</strong>
            <span class="metric-foot">占全部 {{ formatPercent(negativeCount, overview.total) }}</span>
          </div>
        </div>
        <div class="metric-card">
          <div class="metric-icon amber"><el-icon><BellFilled /></el-icon></div>
          <div class="metric-copy">
            <span class="metric-label">风险信息</span>
            <strong class="metric-value">{{ formatCount(riskCount) }}</strong>
            <span class="metric-foot">重点关注 {{ formatCount(focusRiskCount) }} 条</span>
          </div>
        </div>
        <div class="metric-card">
          <div class="metric-icon green"><el-icon><TrendCharts /></el-icon></div>
          <div class="metric-copy">
            <span class="metric-label">互动总量</span>
            <strong class="metric-value">{{ formatCompactCount(interactionTotal) }}</strong>
            <span class="metric-foot">点赞 {{ formatCompactCount(sums.likeCount) }} · 评论 {{ formatCompactCount(sums.commentCount) }}</span>
          </div>
        </div>
      </div>

      <div v-if="!loading && overview.total === 0" class="stats-empty">
        <el-empty description="当前筛选条件下暂无统计数据" :image-size="64" />
      </div>

      <template v-else>
        <div class="chart-grid">
          <section class="chart-card trend-card">
            <div class="chart-head">
              <div>
                <h3>信息趋势</h3>
                <p>{{ trendRangeLabel }}</p>
              </div>
              <span class="chart-unit">单位：条</span>
            </div>
            <div v-if="trendPoints.length" class="trend-chart">
              <svg viewBox="0 0 720 220" preserveAspectRatio="xMidYMid meet" role="img" aria-label="信息趋势">
                <defs>
                  <linearGradient id="statsTrendFill" x1="0" y1="0" x2="0" y2="1">
                    <stop offset="0%" stop-color="#3b82f6" stop-opacity="0.22" />
                    <stop offset="100%" stop-color="#3b82f6" stop-opacity="0" />
                  </linearGradient>
                </defs>
                <g class="grid-lines">
                  <line
                    v-for="line in gridLines"
                    :key="line.y"
                    x1="36"
                    :y1="line.y"
                    x2="684"
                    :y2="line.y"
                  />
                  <text
                    v-for="line in gridLines"
                    :key="`label-${line.y}`"
                    x="28"
                    :y="line.y + 3"
                    text-anchor="end"
                  >{{ line.label }}</text>
                </g>
                <path :d="areaPath" fill="url(#statsTrendFill)" />
                <path :d="linePath" fill="none" stroke="#2f6fed" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round" />
                <g v-for="point in trendPoints" :key="point.key">
                  <circle
                    :cx="point.x"
                    :cy="point.y"
                    r="3.2"
                    fill="#fff"
                    stroke="#2f6fed"
                    stroke-width="2"
                    class="trend-dot"
                  >
                    <title>{{ point.label }}：{{ point.count }} 条</title>
                  </circle>
                </g>
                <g class="x-labels">
                  <text
                    v-for="label in xLabels"
                    :key="label.key"
                    :x="label.x"
                    y="210"
                    text-anchor="middle"
                  >{{ label.text }}</text>
                </g>
              </svg>
            </div>
            <el-empty v-else description="暂无趋势数据" :image-size="56" />
          </section>

          <section class="chart-card">
            <div class="chart-head">
              <div>
                <h3>情感分布</h3>
                <p>按信息情感倾向统计</p>
              </div>
            </div>
            <div class="donut-wrap">
              <div class="donut" :style="donutStyle">
                <div class="donut-hole">
                  <strong>{{ formatCount(overview.total) }}</strong>
                  <span>信息量</span>
                </div>
              </div>
              <div class="donut-legend">
                <div v-for="item in emotionRows" :key="item.value" class="legend-row">
                  <span class="legend-dot" :style="{ background: item.color }"></span>
                  <span class="legend-name">{{ item.label }}</span>
                  <span class="legend-value">{{ formatCount(item.count) }}</span>
                  <span class="legend-percent">{{ formatPercent(item.count, overview.total) }}</span>
                </div>
              </div>
            </div>
          </section>

          <section class="chart-card">
            <div class="chart-head">
              <div>
                <h3>平台分布</h3>
                <p>信息来源平台 Top 6</p>
              </div>
            </div>
            <div v-if="sourceRows.length" class="bar-list">
              <div v-for="row in sourceRows" :key="row.label" class="bar-row">
                <span class="bar-label one-line" :title="row.label">{{ row.label }}</span>
                <div class="bar-track">
                  <span class="bar-fill" :style="{ width: `${row.percent}%`, background: row.color }"></span>
                </div>
                <span class="bar-count">{{ formatCount(row.count) }}</span>
              </div>
            </div>
            <el-empty v-else description="暂无平台数据" :image-size="52" />
          </section>

          <section class="chart-card">
            <div class="chart-head">
              <div>
                <h3>风险分布</h3>
                <p>按风险等级统计</p>
              </div>
            </div>
            <div v-if="riskRows.length" class="bar-list">
              <div v-for="row in riskRows" :key="row.label" class="bar-row">
                <span class="bar-label one-line">{{ row.label }}</span>
                <div class="bar-track">
                  <span class="bar-fill" :style="{ width: `${row.percent}%`, background: row.color }"></span>
                </div>
                <span class="bar-count">{{ formatCount(row.count) }}</span>
              </div>
            </div>
            <el-empty v-else description="暂无风险数据" :image-size="52" />
          </section>
        </div>

        <p v-if="note" class="stats-note">
          <el-icon><InfoFilled /></el-icon>
          {{ note }}
        </p>
      </template>
    </template>
  </div>
</template>

<script setup>
import { computed, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import {
  BellFilled,
  Document,
  InfoFilled,
  TrendCharts,
  Warning
} from '@element-plus/icons-vue'
import { overviewStats, sumStats, trendStats } from '../api'
import { EMOTION_OPTIONS, RISK_OPTIONS } from '../constants'
import {
  formatChartTime,
  formatCompactCount,
  formatCount,
  formatPercent,
  pickTrendInterval,
  platformByLabel
} from '../utils/format'

const props = defineProps({
  query: { type: Object, default: null },
  active: { type: Boolean, default: false },
  refreshKey: { type: Number, default: 0 },
  note: { type: String, default: '' }
})

const loading = ref(false)
const error = ref('')
const overview = ref({ total: 0, sources: [], emotions: [], riskLevels: [] })
const sums = ref({
  repostCount: 0,
  commentCount: 0,
  likeCount: 0,
  followerCount: 0,
  collectCount: 0
})
const trend = ref({ buckets: [], interval: 'day' })

const negativeCount = computed(() => (
  (overview.value.emotions || []).find((item) => item.key === '负面')?.count || 0
))

const riskCount = computed(() => (
  (overview.value.riskLevels || []).reduce((sum, item) => sum + Number(item.count || 0), 0)
))

const focusRiskCount = computed(() => (
  (overview.value.riskLevels || [])
    .filter((item) => item.key === '重点关注')
    .reduce((sum, item) => sum + Number(item.count || 0), 0)
))

const interactionTotal = computed(() => {
  const value = sums.value || {}
  return Number(value.repostCount || 0)
    + Number(value.commentCount || 0)
    + Number(value.likeCount || 0)
    + Number(value.collectCount || 0)
})

const emotionRows = computed(() => {
  const map = Object.fromEntries(
    (overview.value.emotions || []).map((item) => [item.key, Number(item.count || 0)])
  )
  return EMOTION_OPTIONS.map((item) => ({
    ...item,
    count: map[item.value] || 0
  }))
})

const donutStyle = computed(() => {
  const rows = emotionRows.value.filter((item) => item.count > 0)
  const total = rows.reduce((sum, item) => sum + item.count, 0)
  if (!total) return { background: '#eef2f8' }
  let cursor = 0
  const parts = rows.map((item) => {
    const start = (cursor / total) * 360
    cursor += item.count
    const end = (cursor / total) * 360
    return `${item.color} ${start}deg ${end}deg`
  })
  return { background: `conic-gradient(${parts.join(', ')})` }
})

const sourceRows = computed(() => {
  const total = Number(overview.value.total || 0)
  return (overview.value.sources || [])
    .slice(0, 6)
    .map((item) => {
      const label = item.key || '未知'
      const platform = platformByLabel(label)
      return {
        label,
        count: Number(item.count || 0),
        percent: total ? Math.max(3, (Number(item.count || 0) / total) * 100) : 0,
        color: platform?.color || '#5b86f0'
      }
    })
})

const riskRows = computed(() => {
  const total = Number(overview.value.total || 0)
  const map = Object.fromEntries(
    (overview.value.riskLevels || []).map((item) => [item.key, Number(item.count || 0)])
  )
  return RISK_OPTIONS
    .map((item) => ({
      label: item.label,
      count: map[item.value] || 0,
      percent: total ? Math.max(3, ((map[item.value] || 0) / total) * 100) : 0,
      color: item.color
    }))
    .filter((item) => item.count > 0)
})

const trendPoints = computed(() => {
  const buckets = trend.value.buckets || []
  if (!buckets.length) return []
  const width = 720
  const height = 220
  const padX = 36
  const padTop = 18
  const padBottom = 32
  const max = Math.max(1, ...buckets.map((item) => Number(item.count || 0)))
  const innerWidth = width - padX * 2
  const innerHeight = height - padTop - padBottom
  const step = buckets.length > 1 ? innerWidth / (buckets.length - 1) : 0
  return buckets.map((item, index) => {
    const x = buckets.length === 1 ? padX + innerWidth / 2 : padX + index * step
    return {
      key: item.key || index,
      x,
      y: padTop + innerHeight - (Number(item.count || 0) / max) * innerHeight,
      count: Number(item.count || 0),
      label: formatChartTime(item.key, trend.value.interval)
    }
  })
})

const gridLines = computed(() => {
  const max = Math.max(1, ...(trend.value.buckets || []).map((item) => Number(item.count || 0)))
  return [0, 0.25, 0.5, 0.75, 1].map((ratio) => ({
    y: 18 + (1 - ratio) * 170,
    label: String(Math.round(max * ratio))
  }))
})

const linePath = computed(() => trendPoints.value
  .map((point, index) => `${index === 0 ? 'M' : 'L'} ${point.x.toFixed(2)} ${point.y.toFixed(2)}`)
  .join(' '))

const areaPath = computed(() => {
  if (!trendPoints.value.length) return ''
  const base = 188
  const first = trendPoints.value[0]
  const last = trendPoints.value[trendPoints.value.length - 1]
  return `${linePath.value} L ${last.x.toFixed(2)} ${base} L ${first.x.toFixed(2)} ${base} Z`
})

const xLabels = computed(() => {
  const points = trendPoints.value
  if (!points.length) return []
  if (points.length <= 6) {
    return points.map((point) => ({ key: point.key, x: point.x, text: point.label }))
  }
  const indexes = [0, Math.floor(points.length / 2), points.length - 1]
  return indexes.map((index) => ({
    key: points[index].key,
    x: points[index].x,
    text: points[index].label
  }))
})

const trendRangeLabel = computed(() => (
  trend.value.interval === 'hour' ? '按小时统计'
    : trend.value.interval === 'week' ? '按周统计'
      : trend.value.interval === 'month' ? '按月统计'
        : '按天统计'
))

const queryKey = computed(() => JSON.stringify(props.query || {}))

function cleanPayload(payload) {
  return Object.fromEntries(
    Object.entries(payload).filter(([, value]) => {
      if (value === null || value === undefined || value === '') return false
      if (Array.isArray(value) && !value.length) return false
      return true
    })
  )
}

function buildBasePayload() {
  const query = props.query || {}
  return cleanPayload({
    monitorType: 0,
    searchScope: 'FULL_TEXT',
    keywords: query.keywords || null,
    excludeKeywords: query.excludeKeywords || null,
    startTime: query.startTime || null,
    endTime: query.endTime || null,
    sources: query.sources || [],
    emotions: query.emotions || [],
    riskLevels: query.riskLevels || []
  })
}

async function load() {
  if (!props.active || !props.query) return
  loading.value = true
  error.value = ''
  try {
    const base = buildBasePayload()
    const interval = pickTrendInterval(props.query?.startTime, props.query?.endTime)
    const [overviewData, sumData, trendData] = await Promise.all([
      overviewStats({ ...base, topSize: 30 }),
      sumStats(base),
      trendStats({ ...base, timeField: 'createdAt', interval })
    ])
    overview.value = overviewData || { total: 0, sources: [], emotions: [], riskLevels: [] }
    sums.value = sumData?.sums || {
      repostCount: 0,
      commentCount: 0,
      likeCount: 0,
      followerCount: 0,
      collectCount: 0
    }
    trend.value = trendData || { buckets: [], interval }
  } catch (err) {
    error.value = err.message || '统计数据加载失败'
    ElMessage.error(error.value)
  } finally {
    loading.value = false
  }
}

watch([() => props.active, () => props.refreshKey, queryKey], () => {
  if (props.active) load()
}, { immediate: true })
</script>

<style scoped>
.stats-panel {
  padding: 16px 20px 24px;
}

.metric-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
}

.metric-card {
  display: flex;
  align-items: center;
  gap: 13px;
  padding: 15px 16px;
  background: #fff;
  border: 1px solid var(--border);
  border-radius: var(--radius);
  box-shadow: var(--shadow-sm);
}

.metric-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 40px;
  height: 40px;
  flex-shrink: 0;
  font-size: 18px;
  border-radius: 11px;
}

.metric-icon.blue {
  color: #2563eb;
  background: #edf3ff;
}

.metric-icon.red {
  color: #e5484d;
  background: #fff0ef;
}

.metric-icon.amber {
  color: #d97706;
  background: #fff7e8;
}

.metric-icon.green {
  color: #13946b;
  background: #ebfaf4;
}

.metric-copy {
  display: flex;
  flex-direction: column;
  min-width: 0;
  line-height: 1.3;
}

.metric-label {
  font-size: 11.5px;
  color: var(--text-muted);
}

.metric-value {
  margin-top: 3px;
  font-size: 23px;
  font-weight: 700;
  color: #17263d;
  letter-spacing: 0.2px;
}

.metric-foot {
  margin-top: 2px;
  overflow: hidden;
  font-size: 10.5px;
  color: #98a4b6;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.chart-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
  margin-top: 12px;
}

.chart-card {
  padding: 15px 17px 14px;
  background: #fff;
  border: 1px solid var(--border);
  border-radius: var(--radius);
  box-shadow: var(--shadow-sm);
}

.trend-card {
  grid-column: span 3;
}

.chart-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 8px;
}

.chart-head h3 {
  margin: 0;
  font-size: 13.5px;
  font-weight: 600;
  color: #263750;
}

.chart-head p {
  margin: 4px 0 0;
  font-size: 10.5px;
  color: var(--text-muted);
}

.chart-unit {
  font-size: 10.5px;
  color: #a1acbc;
}

.trend-chart {
  margin-top: 8px;
}

.trend-chart svg {
  display: block;
  width: 100%;
  height: 232px;
  overflow: visible;
}

.grid-lines line {
  stroke: #eef2f8;
  stroke-width: 1;
}

.grid-lines text,
.x-labels text {
  font-size: 9px;
  fill: #a3aec0;
}

.trend-dot {
  transition: r 0.15s ease;
}

.trend-dot:hover {
  r: 5;
}

.donut-wrap {
  display: flex;
  align-items: center;
  gap: 18px;
  padding: 14px 2px 4px;
}

.donut {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 116px;
  height: 116px;
  flex-shrink: 0;
  border-radius: 50%;
}

.donut-hole {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  width: 78px;
  height: 78px;
  background: #fff;
  border-radius: 50%;
  box-shadow: inset 0 0 0 1px #f2f5fa;
}

.donut-hole strong {
  font-size: 17px;
  color: #1d2b42;
}

.donut-hole span {
  margin-top: 1px;
  font-size: 10px;
  color: var(--text-muted);
}

.donut-legend {
  display: flex;
  flex: 1;
  flex-direction: column;
  gap: 9px;
  min-width: 0;
}

.legend-row {
  display: flex;
  align-items: center;
  gap: 7px;
  font-size: 11.5px;
}

.legend-dot {
  width: 7px;
  height: 7px;
  flex-shrink: 0;
  border-radius: 50%;
}

.legend-name {
  flex: 1;
  color: var(--text-secondary);
}

.legend-value {
  color: #31435e;
  font-weight: 600;
}

.legend-percent {
  width: 44px;
  color: var(--text-muted);
  text-align: right;
}

.bar-list {
  display: flex;
  flex-direction: column;
  gap: 11px;
  padding: 15px 0 4px;
}

.bar-row {
  display: flex;
  align-items: center;
  gap: 9px;
}

.bar-label {
  width: 62px;
  flex-shrink: 0;
  font-size: 11px;
  color: var(--text-secondary);
}

.bar-track {
  flex: 1;
  height: 7px;
  overflow: hidden;
  background: #f1f4f9;
  border-radius: 999px;
}

.bar-fill {
  display: block;
  height: 100%;
  border-radius: 999px;
  transition: width 0.4s ease;
}

.bar-count {
  width: 38px;
  flex-shrink: 0;
  font-size: 11px;
  color: #475971;
  text-align: right;
}

.stats-note {
  display: flex;
  align-items: center;
  gap: 6px;
  margin: 14px 2px 0;
  font-size: 11px;
  color: var(--text-muted);
}

.stats-empty {
  margin-top: 10px;
}

.stats-error {
  padding: 20px 0;
}

.error-detail {
  margin: 8px 0 0;
  font-size: 11px;
  color: var(--text-muted);
}

@media (max-width: 1400px) {
  .metric-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .chart-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .trend-card {
    grid-column: span 2;
  }
}
</style>