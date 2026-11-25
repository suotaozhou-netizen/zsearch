<template>
  <div class="search-page">
    <div class="page-inner">
      <section class="filter-card">
        <div class="keyword-row">
          <el-input
            v-model="filters.keywords"
            size="large"
            placeholder="输入关键词，支持 + 同时包含、| 任一包含、* 邻近检索、() 组合条件"
            clearable
            class="keyword-input"
            @keyup.enter="handleSearch"
          >
            <template #prefix><el-icon><Search /></el-icon></template>
          </el-input>
          <el-select v-model="filters.scope" size="large" class="scope-select">
            <el-option
              v-for="item in SEARCH_SCOPE_OPTIONS"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
          <el-button type="primary" size="large" class="search-btn" @click="handleSearch">
            搜索
          </el-button>
          <el-button size="large" @click="handleReset">重置</el-button>
        </div>

        <div class="filter-row">
          <span class="filter-label">时间范围</span>
          <el-radio-group v-model="filters.timeRange" @change="handleTimeChange">
            <el-radio-button
              v-for="item in TIME_RANGE_OPTIONS"
              :key="item.value"
              :value="item.value"
            >
              {{ item.label }}
            </el-radio-button>
          </el-radio-group>
          <el-date-picker
            v-if="isCustom"
            v-model="filters.customRange"
            type="datetimerange"
            value-format="YYYY-MM-DD HH:mm:ss"
            range-separator="至"
            start-placeholder="开始时间"
            end-placeholder="结束时间"
            class="custom-time"
          />

          <div class="filter-spacer"></div>

          <el-select v-model="filters.sort" class="sort-select">
            <template #prefix><el-icon><Sort /></el-icon></template>
            <el-option
              v-for="item in SORT_OPTIONS"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
          <el-button :icon="Filter" @click="advanced = !advanced">
            {{ advanced ? '收起筛选' : '高级筛选' }}
            <el-icon class="toggle-arrow" :class="{ up: advanced }"><ArrowDown /></el-icon>
          </el-button>
        </div>

        <div class="media-row">
          <span class="filter-label">媒体类型</span>
          <el-checkbox-group v-model="filters.sources" class="media-group">
            <el-checkbox-button
              v-for="site in SITE_OPTIONS"
              :key="site.code"
              :value="site.label"
            >
              <span class="media-option">
                {{ site.label }}
                <span v-if="siteCountReady" class="media-count">{{ siteCounts[String(site.code)] ?? 0 }}</span>
              </span>
            </el-checkbox-button>
          </el-checkbox-group>
        </div>

        <div v-show="advanced" class="advanced-row">
          <div class="advanced-item">
            <span class="filter-label">排除词</span>
            <el-input
              v-model="filters.excludeKeywords"
              placeholder="排除包含这些词的信息"
              clearable
              class="advanced-input"
              @keyup.enter="handleSearch"
            />
          </div>
          <div class="advanced-item">
            <span class="filter-label">情感</span>
            <el-select
              v-model="filters.emotions"
              multiple
              collapse-tags
              collapse-tags-tooltip
              clearable
              placeholder="全部情感"
              class="advanced-select narrow"
            >
              <el-option
                v-for="item in EMOTION_OPTIONS"
                :key="item.value"
                :label="item.label"
                :value="item.value"
              />
            </el-select>
          </div>
          <div class="advanced-item">
            <span class="filter-label">风险</span>
            <el-select
              v-model="filters.riskLevels"
              multiple
              collapse-tags
              collapse-tags-tooltip
              clearable
              placeholder="全部等级"
              class="advanced-select narrow"
            >
              <el-option
                v-for="item in RISK_OPTIONS"
                :key="item.value"
                :label="item.label"
                :value="item.value"
              />
            </el-select>
          </div>
        </div>
      </section>

      <section class="result-card">
        <div class="result-head">
          <div class="result-title-wrap">
            <h2>检索结果</h2>
            <span class="result-total">共 {{ formatCount(total) }} 条</span>
            <span v-if="appliedKeywords" class="result-keyword">
              <el-icon><Search /></el-icon>{{ appliedKeywords }}
            </span>
          </div>
          <el-button
            size="small"
            :icon="Download"
            :disabled="!articles.length"
            @click="exportResults"
          >
            导出结果
          </el-button>
        </div>

        <div class="result-body">
          <ArticleFeed
            :articles="articles"
            :total="total"
            :page="pagination.page"
            :page-size="pagination.size"
            :loading="loading"
            :highlight-terms="highlightTerms"
            empty-text="暂无匹配信息"
            empty-tip="可以尝试缩短关键词、扩大时间范围或减少筛选条件"
            show-index
            @update:page="handlePageChange"
            @update:page-size="handleSizeChange"
          />
        </div>
      </section>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { ArrowDown, Download, Filter, Search, Sort } from '@element-plus/icons-vue'
import ArticleFeed from '../components/ArticleFeed.vue'
import { searchArticles, siteCountStats } from '../api'
import {
  EMOTION_OPTIONS,
  RISK_OPTIONS,
  SEARCH_SCOPE_OPTIONS,
  SITE_OPTIONS,
  SITE_SOURCE_BY_LABEL,
  SORT_OPTIONS,
  TIME_RANGE_OPTIONS
} from '../constants'
import {
  downloadText,
  formatCount,
  formatDateTime,
  parseKeywordTerms,
  resolveTimeRange,
  siteLabel,
  toCsv
} from '../utils/format'

const filters = reactive({
  timeRange: 'LAST_7_DAYS',
  customRange: [],
  keywords: '',
  excludeKeywords: '',
  scope: 'FULL_TEXT',
  sources: [],
  emotions: [],
  riskLevels: [],
  sort: 'createdAt:desc'
})

const pagination = reactive({ page: 1, size: 10 })
const articles = ref([])
const total = ref(0)
const loading = ref(false)
const siteCounts = ref({})
const siteCountReady = ref(false)
const advanced = ref(false)
const appliedKeywords = ref('')
const appliedExclude = ref('')

const isCustom = computed(() => filters.timeRange === 'CUSTOM')
const highlightTerms = computed(() => [
  ...parseKeywordTerms(appliedKeywords.value),
  ...parseKeywordTerms(appliedExclude.value)
])

function buildPayload() {
  const { startTime, endTime } = resolveTimeRange(filters.timeRange, filters.customRange)
  const [sortBy, sortOrder] = filters.sort.split(':')
  const payload = {
    monitorType: 0,
    searchScope: filters.scope,
    page: pagination.page,
    size: pagination.size,
    sortBy,
    sortOrder
  }
  if (filters.keywords.trim()) payload.keywords = filters.keywords.trim()
  if (filters.excludeKeywords.trim()) payload.excludeKeywords = filters.excludeKeywords.trim()
  if (startTime) payload.startTime = startTime
  if (endTime) payload.endTime = endTime
  if (filters.sources.length) {
    payload.sources = filters.sources.map((label) => SITE_SOURCE_BY_LABEL[label] || label)
  }
  if (filters.emotions.length) payload.emotions = filters.emotions
  if (filters.riskLevels.length) payload.riskLevels = filters.riskLevels
  return payload
}

function buildCountPayload() {
  const payload = buildPayload()
  delete payload.page
  delete payload.size
  delete payload.sortBy
  delete payload.sortOrder
  delete payload.sources
  return payload
}

let siteCountRequestId = 0

async function fetchSiteCounts() {
  const requestId = ++siteCountRequestId
  try {
    const data = await siteCountStats(buildCountPayload())
    if (requestId !== siteCountRequestId) return
    siteCounts.value = data || {}
    siteCountReady.value = true
  } catch {
    if (requestId !== siteCountRequestId) return
    siteCounts.value = {}
    siteCountReady.value = false
  }
}

async function fetchList() {
  if (isCustom.value && filters.customRange?.length !== 2) {
    ElMessage.warning('请选择完整的自定义时间范围')
    return
  }
  appliedKeywords.value = filters.keywords.trim()
  appliedExclude.value = filters.excludeKeywords.trim()
  loading.value = true
  fetchSiteCounts()
  try {
    const data = await searchArticles(buildPayload())
    articles.value = data?.articleList || []
    total.value = data?.total || 0
  } catch (error) {
    articles.value = []
    total.value = 0
    ElMessage.error(error.message)
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  pagination.page = 1
  fetchList()
}

function handleReset() {
  filters.timeRange = 'LAST_7_DAYS'
  filters.customRange = []
  filters.keywords = ''
  filters.excludeKeywords = ''
  filters.scope = 'FULL_TEXT'
  filters.sources = []
  filters.emotions = []
  filters.riskLevels = []
  filters.sort = 'createdAt:desc'
  pagination.page = 1
  fetchList()
}

function handleTimeChange(value) {
  if (value !== 'CUSTOM') filters.customRange = []
}

function handlePageChange(value) {
  pagination.page = value
  fetchList()
}

function handleSizeChange(value) {
  pagination.size = value
  pagination.page = 1
  fetchList()
}

async function exportResults() {
  loading.value = true
  try {
    const payload = { ...buildPayload(), page: 1, size: 100 }
    const all = []
    for (let page = 1; page <= 5; page += 1) {
      const data = await searchArticles({ ...payload, page })
      const rows = data?.articleList || []
      all.push(...rows)
      if (!rows.length || all.length >= (data?.total || 0)) break
    }
    const rows = all.slice(0, 500)
    const csv = toCsv(rows, [
      { label: '标题', value: 'title' },
      { label: '作者', value: 'name' },
      { label: '平台', value: (row) => siteLabel(row) },
      { label: '情感', value: 'emotion' },
      { label: '风险等级', value: 'riskLevel' },
      { label: '发布时间', value: (row) => formatDateTime(row.createdAt) },
      { label: '点赞', value: 'likeCount' },
      { label: '评论', value: 'commentCount' },
      { label: '转发', value: 'repostCount' },
      { label: '链接', value: 'url' }
    ])
    downloadText(`信息搜索结果_${formatDateTime(new Date()).replace(/[: ]/g, '-')}.csv`, csv)
    ElMessage.success(`已导出 ${rows.length} 条信息`)
  } catch (error) {
    ElMessage.error(error.message || '导出失败')
  } finally {
    loading.value = false
  }
}

onMounted(fetchList)
</script>

<style scoped>
.search-page {
  display: flex;
  height: 100%;
  overflow: hidden;
}

.page-inner {
  display: flex;
  flex: 1;
  flex-direction: column;
  gap: 14px;
  min-width: 0;
  min-height: 0;
  padding: 18px 24px 20px;
  box-sizing: border-box;
}

.filter-card,
.result-card {
  background: #fff;
  border: 1px solid var(--border);
  border-radius: var(--radius);
  box-shadow: var(--shadow-sm);
}

.filter-card {
  flex-shrink: 0;
  padding: 18px 20px 16px;
}

.keyword-row {
  display: flex;
  gap: 10px;
}

.keyword-input {
  flex: 1;
}

.scope-select {
  width: 106px;
}

.search-btn {
  min-width: 92px;
}

.filter-row {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 10px;
  padding-top: 15px;
  margin-top: 15px;
  border-top: 1px dashed #e8eef7;
}

.filter-label {
  flex-shrink: 0;
  font-size: 12px;
  color: var(--text-muted);
}

.custom-time {
  width: 340px;
}

.filter-spacer {
  flex: 1;
}

.sort-select {
  width: 122px;
}

.toggle-arrow {
  margin-left: 2px;
  font-size: 11px;
  transition: transform 0.18s ease;
}

.toggle-arrow.up {
  transform: rotate(180deg);
}

.media-row {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 10px;
  padding-top: 15px;
  margin-top: 15px;
  border-top: 1px dashed #e8eef7;
}

.media-group {
  flex-wrap: wrap;
}

.media-option {
  display: inline-flex;
  align-items: center;
  gap: 6px;
}

.media-count {
  padding: 0 6px;
  font-size: 11px;
  line-height: 16px;
  color: #8a97ab;
  background: #f3f6fb;
  border-radius: 999px;
}

.media-group :deep(.el-checkbox-button.is-checked) .media-count {
  color: #fff;
  background: rgba(255, 255, 255, 0.24);
}

.advanced-row {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 13px 20px;
  padding-top: 14px;
  margin-top: 14px;
  border-top: 1px dashed #e8eef7;
}

.advanced-item {
  display: flex;
  align-items: center;
  gap: 8px;
}

.advanced-input {
  width: 220px;
}

.advanced-select {
  width: 190px;
}

.advanced-select.narrow {
  width: 150px;
}

.result-card {
  display: flex;
  flex: 1;
  flex-direction: column;
  min-height: 0;
  padding: 16px 20px 0;
}

.result-head {
  display: flex;
  flex-shrink: 0;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding-bottom: 12px;
  border-bottom: 1px solid #f0f4fa;
}

.result-body {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
}

.result-title-wrap {
  display: flex;
  align-items: baseline;
  flex-wrap: wrap;
  gap: 10px;
}

.result-title-wrap h2 {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
}

.result-total {
  font-size: 12px;
  color: var(--text-muted);
}

.result-keyword {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 2px 8px;
  font-size: 11px;
  color: var(--brand);
  background: var(--brand-soft);
  border-radius: 999px;
}

.result-keyword .el-icon {
  font-size: 11px;
}

</style>