<template>
  <div class="export-page">
    <section class="panel">
      <div class="panel-head">
        <div class="panel-title">新建导出</div>
        <div class="panel-actions">
          <el-button size="small" :loading="realtimeLoading" @click="handleRealtime">
            实时导出（前1000条）
          </el-button>
          <el-button type="primary" size="small" :loading="creating" @click="handleCreateTask">
            创建离线任务（上限10万条）
          </el-button>
        </div>
      </div>

      <div class="row">
        <span class="label">监测类型</span>
        <el-radio-group v-model="monitorType" size="small">
          <el-radio-button
            v-for="item in MONITOR_TYPES"
            :key="item.value"
            :value="item.value"
          >
            {{ item.label }}
          </el-radio-button>
        </el-radio-group>
        <el-select
          v-if="monitorType !== 0"
          v-model="form.settingId"
          size="small"
          clearable
          filterable
          placeholder="选择监测方案"
          class="setting-select"
        >
          <el-option-group
            v-for="group in settingGroups"
            :key="group.name"
            :label="group.name"
          >
            <el-option
              v-for="setting in group.settings"
              :key="setting.id"
              :label="setting.name"
              :value="setting.id"
            />
          </el-option-group>
        </el-select>
      </div>

      <div class="row">
        <span class="label">关键词</span>
        <el-input
          v-model="form.keywords"
          size="small"
          clearable
          placeholder="支持 + | * () 表达式"
          class="text-input"
        />
        <span class="label">排除词</span>
        <el-input
          v-model="form.excludeKeywords"
          size="small"
          clearable
          placeholder="命中即排除"
          class="text-input"
        />
      </div>

      <div class="row">
        <span class="label">时间范围</span>
        <el-radio-group v-model="form.timeRange" size="small">
          <el-radio-button
            v-for="item in TIME_RANGE_OPTIONS"
            :key="item.value"
            :value="item.value"
          >
            {{ item.label }}
          </el-radio-button>
        </el-radio-group>
        <el-date-picker
          v-if="form.timeRange === 'CUSTOM'"
          v-model="form.customRange"
          type="datetimerange"
          size="small"
          value-format="YYYY-MM-DD HH:mm:ss"
          range-separator="至"
          start-placeholder="开始时间"
          end-placeholder="结束时间"
          class="custom-time"
        />
      </div>

      <div class="row">
        <span class="label">媒体类型</span>
        <el-checkbox-group v-model="form.sources" class="media-group">
          <el-checkbox-button
            v-for="site in SITE_OPTIONS"
            :key="site.code"
            :value="site.label"
          >
            {{ site.label }}
          </el-checkbox-button>
        </el-checkbox-group>
      </div>

      <div class="row">
        <span class="label">情感</span>
        <el-select
          v-model="form.emotions"
          size="small"
          multiple
          clearable
          collapse-tags
          collapse-tags-tooltip
          placeholder="全部情感"
          class="narrow-select"
        >
          <el-option
            v-for="item in EMOTION_OPTIONS"
            :key="item.value"
            :label="item.label"
            :value="item.value"
          />
        </el-select>
        <span class="label">风险</span>
        <el-select
          v-model="form.riskLevels"
          size="small"
          multiple
          clearable
          collapse-tags
          collapse-tags-tooltip
          placeholder="全部等级"
          class="narrow-select"
        >
          <el-option
            v-for="item in RISK_OPTIONS"
            :key="item.value"
            :label="item.label"
            :value="item.value"
          />
        </el-select>
      </div>

      <div class="row">
        <span class="label">导出字段</span>
        <el-select
          v-model="selectedFields"
          size="small"
          multiple
          collapse-tags
          collapse-tags-tooltip
          placeholder="选择导出字段"
          class="fields-select"
        >
          <el-option
            v-for="field in fieldOptions"
            :key="field.key"
            :label="field.header"
            :value="field.key"
          />
        </el-select>
        <el-button link size="small" class="reset-fields" @click="resetFields">恢复默认</el-button>
      </div>
    </section>

    <section class="panel task-panel">
      <div class="panel-head">
        <div class="panel-title">导出任务</div>
        <div class="panel-actions">
          <span class="hint">仅显示最近 50 条</span>
          <el-button size="small" :icon="Refresh" :loading="listLoading" @click="loadTasks()">
            刷新
          </el-button>
        </div>
      </div>
      <el-table
        v-loading="listLoading"
        :data="tasks"
        size="small"
        class="task-table"
        empty-text="暂无导出任务"
      >
        <el-table-column prop="taskName" label="任务名称" min-width="180" show-overflow-tooltip />
        <el-table-column label="模块" width="110">
          <template #default="{ row }">{{ monitorTypeText(row.monitorType) }}</template>
        </el-table-column>
        <el-table-column label="数据量" width="130">
          <template #default="{ row }">
            {{ row.exportedRows ?? 0 }} / {{ row.totalRows ?? 0 }}
          </template>
        </el-table-column>
        <el-table-column label="状态" width="120">
          <template #default="{ row }">
            <el-tooltip
              :disabled="!row.errorMsg"
              :content="row.errorMsg || ''"
              placement="top"
            >
              <el-tag size="small" :type="statusTagType(row.status)">
                {{ row.statusText || statusText(row.status) }}
              </el-tag>
            </el-tooltip>
          </template>
        </el-table-column>
        <el-table-column label="创建时间" width="170">
          <template #default="{ row }">{{ formatDateTime(row.createTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="90" fixed="right">
          <template #default="{ row }">
            <el-button
              link
              type="primary"
              size="small"
              :disabled="row.status !== 2"
              :loading="downloadingId === row.id"
              @click="handleDownload(row)"
            >
              下载
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </section>
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { Refresh } from '@element-plus/icons-vue'
import {
  createExportTask,
  downloadExportTask,
  exportRealtime,
  fetchExportFields,
  fetchExportTasks,
  fetchMonitorTree,
  fetchSourceMonitorTree
} from '../api'
import {
  EMOTION_OPTIONS,
  RISK_OPTIONS,
  SITE_OPTIONS,
  SITE_SOURCE_BY_LABEL,
  TIME_RANGE_OPTIONS
} from '../constants'
import { downloadBlob, formatDateTime, resolveTimeRange } from '../utils/format'

const MONITOR_TYPES = [
  { value: 0, label: '通用检索' },
  { value: 1, label: '关键词监测' },
  { value: 2, label: '定向监测' }
]

const DEFAULT_FIELDS = ['title', 'name', 'source', 'url', 'createdAt', 'emotion', 'riskLevel']

const FALLBACK_FIELDS = [
  { key: 'title', header: '标题' },
  { key: 'text', header: '正文' },
  { key: 'name', header: '用户名' },
  { key: 'source', header: '信息来源' },
  { key: 'url', header: '原文链接' },
  { key: 'imageList', header: '原文图片' },
  { key: 'videoList', header: '原文视频' },
  { key: 'createdAt', header: '发布时间' },
  { key: 'emotion', header: '情感类型' },
  { key: 'riskLevel', header: '风险等级' },
  { key: 'ocr', header: 'OCR' },
  { key: 'asr', header: 'ASR' },
  { key: 'repostCount', header: '转发数' },
  { key: 'commentCount', header: '评论数' },
  { key: 'likeCount', header: '点赞数' },
  { key: 'followerCount', header: '粉丝数' },
  { key: 'collectCount', header: '收藏数' }
]

const STATUS_META = {
  0: { text: '待处理', type: 'info' },
  1: { text: '处理中', type: 'warning' },
  2: { text: '成功', type: 'success' },
  3: { text: '失败', type: 'danger' }
}

const monitorType = ref(0)
const keywordGroups = ref([])
const sourceGroups = ref([])
const fieldOptions = ref([...FALLBACK_FIELDS])
const selectedFields = ref([...DEFAULT_FIELDS])

const form = reactive({
  settingId: null,
  keywords: '',
  excludeKeywords: '',
  timeRange: 'LAST_7_DAYS',
  customRange: [],
  sources: [],
  emotions: [],
  riskLevels: []
})

const tasks = ref([])
const listLoading = ref(false)
const creating = ref(false)
const realtimeLoading = ref(false)
const downloadingId = ref(null)

const settingGroups = computed(() => {
  const groups = monitorType.value === 1 ? keywordGroups.value
    : monitorType.value === 2 ? sourceGroups.value
      : []
  return groups
    .map((group) => ({
      name: group.name,
      settings: (group.settings || []).map((item) => ({ id: item.id, name: item.name }))
    }))
    .filter((group) => group.settings.length)
})

function monitorTypeText(code) {
  return MONITOR_TYPES.find((item) => item.value === code)?.label || '-'
}

function statusText(status) {
  return STATUS_META[status]?.text || '未知'
}

function statusTagType(status) {
  return STATUS_META[status]?.type || 'info'
}

function resetFields() {
  selectedFields.value = [...DEFAULT_FIELDS]
}

function validate() {
  if (!selectedFields.value.length) {
    ElMessage.warning('请选择导出字段')
    return false
  }
  if (monitorType.value !== 0 && !form.settingId) {
    ElMessage.warning('请选择监测方案')
    return false
  }
  if (form.timeRange === 'CUSTOM' && form.customRange?.length !== 2) {
    ElMessage.warning('请选择完整的自定义时间范围')
    return false
  }
  return true
}

function buildPayload() {
  const { startTime, endTime } = resolveTimeRange(form.timeRange, form.customRange)
  const payload = {
    monitorType: monitorType.value,
    searchScope: 'FULL_TEXT',
    sortBy: 'createdAt',
    sortOrder: 'desc',
    fields: [...selectedFields.value]
  }
  if (monitorType.value !== 0) {
    payload.settingId = form.settingId
  }
  if (form.keywords.trim()) payload.keywords = form.keywords.trim()
  if (form.excludeKeywords.trim()) payload.excludeKeywords = form.excludeKeywords.trim()
  if (startTime) payload.startTime = startTime
  if (endTime) payload.endTime = endTime
  if (form.sources.length) {
    payload.sources = form.sources.map((label) => SITE_SOURCE_BY_LABEL[label] || label)
  }
  if (form.emotions.length) payload.emotions = [...form.emotions]
  if (form.riskLevels.length) payload.riskLevels = [...form.riskLevels]
  return payload
}

function stampName(prefix) {
  return `${prefix}_${formatDateTime(new Date()).replace(/[: -]/g, '')}.xlsx`
}

async function handleRealtime() {
  if (!validate()) return
  realtimeLoading.value = true
  try {
    const blob = await exportRealtime({ ...buildPayload(), scope: 'first_1000' })
    downloadBlob(blob, stampName('文章导出'))
    ElMessage.success('实时导出已开始下载')
  } catch (error) {
    ElMessage.error(error.message || '实时导出失败')
  } finally {
    realtimeLoading.value = false
  }
}

async function handleCreateTask() {
  if (!validate()) return
  creating.value = true
  try {
    const task = await createExportTask({ ...buildPayload(), scope: 'all' })
    ElMessage.success(`离线任务已创建，共 ${task?.totalRows ?? 0} 条待导出`)
    await loadTasks()
    startPolling()
  } catch (error) {
    ElMessage.error(error.message || '创建离线任务失败')
  } finally {
    creating.value = false
  }
}

async function handleDownload(row) {
  downloadingId.value = row.id
  try {
    const blob = await downloadExportTask(row.id)
    downloadBlob(blob, `${row.taskName || '文章导出'}.xlsx`)
  } catch (error) {
    ElMessage.error(error.message || '下载失败')
  } finally {
    downloadingId.value = null
  }
}

async function loadTasks(silent = false) {
  if (!silent) listLoading.value = true
  try {
    tasks.value = (await fetchExportTasks()) || []
  } catch (error) {
    if (!silent) ElMessage.error(error.message || '任务加载失败')
  } finally {
    if (!silent) listLoading.value = false
  }
}

let pollTimer = null

function hasActiveTask() {
  return tasks.value.some((task) => task.status === 0 || task.status === 1)
}

function startPolling() {
  if (pollTimer) return
  pollTimer = setInterval(async () => {
    await loadTasks(true)
    if (!hasActiveTask()) stopPolling()
  }, 3000)
}

function stopPolling() {
  if (pollTimer) {
    clearInterval(pollTimer)
    pollTimer = null
  }
}

async function loadTypeGroups(type) {
  try {
    if (type === 1 && !keywordGroups.value.length) {
      keywordGroups.value = (await fetchMonitorTree()) || []
    }
    if (type === 2 && !sourceGroups.value.length) {
      sourceGroups.value = (await fetchSourceMonitorTree()) || []
    }
  } catch (error) {
    ElMessage.error(error.message || '监测方案加载失败')
  }
}

watch(monitorType, (value) => {
  form.settingId = null
  loadTypeGroups(value)
})

onMounted(async () => {
  await loadTasks()
  if (hasActiveTask()) startPolling()
  try {
    const fields = await fetchExportFields()
    if (fields?.length) {
      fieldOptions.value = fields
    }
  } catch (error) {
    console.warn('导出字段加载失败，使用内置字段列表', error)
  }
})

onBeforeUnmount(() => {
  stopPolling()
})
</script>

<style scoped>
.export-page {
  display: flex;
  flex-direction: column;
  gap: 14px;
  height: 100%;
  padding: 18px 24px 22px;
  overflow-y: auto;
  box-sizing: border-box;
}

.panel {
  padding: 14px 20px 16px;
  background: #fff;
  border: 1px solid var(--border);
  border-radius: 14px;
}

.task-panel {
  display: flex;
  flex: 1;
  flex-direction: column;
  min-height: 320px;
}

.panel-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 12px;
}

.panel-title {
  font-size: 14px;
  font-weight: 600;
  color: #1d2b42;
}

.panel-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.hint {
  font-size: 11px;
  color: var(--text-muted);
}

.row {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 10px;
  padding: 9px 0;
  border-top: 1px dashed #e8eef7;
}

.row:first-of-type {
  border-top: none;
}

.label {
  flex-shrink: 0;
  font-size: 12px;
  color: var(--text-muted);
}

.text-input {
  width: 220px;
}

.setting-select {
  width: 220px;
}

.custom-time {
  width: 340px;
}

.narrow-select {
  width: 150px;
}

.fields-select {
  width: 420px;
}

.reset-fields {
  color: var(--text-muted);
}

.media-group {
  flex-wrap: wrap;
}

.task-table {
  flex: 1;
  min-height: 260px;
}
</style>