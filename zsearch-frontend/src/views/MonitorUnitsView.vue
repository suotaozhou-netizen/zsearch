<template>
  <div class="monitor-page">
    <MonitorSidebar
      :groups="groups"
      :selected-id="selectedId"
      :loading="treeLoading"
      :create-disabled="!groups.length"
      create-label="新建监测方案"
      search-placeholder="搜索分组或方案"
      empty-text="暂无监测方案"
      empty-title="先创建分组，再添加关键词监测方案"
      @select="selectUnit"
      @refresh="loadTree"
      @create-group="openGroupDialog()"
      @edit-group="openGroupDialog"
      @delete-group="removeGroup"
      @create-setting="(group) => openSettingDialog({ group })"
      @edit-setting="(item) => openSettingDialog({ setting: item })"
      @delete-setting="removeSetting"
    >
      <template #item-meta="{ item }">{{ unitMeta(item) }}</template>
    </MonitorSidebar>

    <section class="workbench">
      <template v-if="selected">
        <UnitHeader
          :title="selected.name"
          :group-name="selectedGroup?.name"
          :show-crumb="false"
          :show-type-tag="false"
          :show-status="false"
          :active-tab="activeTab"
          :list-total="total"
          :refreshing="loading"
          @update:active-tab="switchTab"
          @refresh="refreshCurrent"
          @edit="openSettingDialog({ setting: selected })"
          @export="exportList"
          @remove="removeSetting(selected)"
        />

        <UnitFilterBar
          v-if="activeTab === 'list'"
          :filters="filters"
          show-keyword
          :site-counts="siteCounts"
          :site-count-ready="siteCountReady"
          @search="handleSearch"
          @reset="handleReset"
          @change="handleFilterChange"
        />

        <div class="workbench-body">
          <template v-if="activeTab === 'list'">
            <div class="list-wrap">
              <div v-if="dirty" class="dirty-bar">
                <el-icon><InfoFilled /></el-icon>
                筛选条件已调整，点击「查询」后刷新结果
              </div>
              <div class="feed-card">
                <ArticleFeed
                  :articles="articles"
                  :total="total"
                  :page="pagination.page"
                  :page-size="pagination.size"
                  :loading="loading"
                  :highlight-terms="highlightTerms"
                  empty-text="暂无匹配信息"
                  empty-tip="可以调整时间范围或方案内筛选条件后重试"
                  @update:page="handlePageChange"
                  @update:page-size="handleSizeChange"
                />
              </div>
            </div>
          </template>

          <StatsPanel
            v-else
            :query="statsQuery"
            :active="activeTab === 'stats'"
            :refresh-key="refreshKey"
            note="监测单元统计基于通用检索按方案关键词与筛选条件聚合，暂不支持按方案直接统计。"
          />
        </div>
      </template>

      <div v-else class="empty-workbench">
        <el-empty description="选择左侧监测方案查看信息，或先创建一个方案">
          <div class="empty-actions">
            <el-button v-if="!groups.length" @click="openGroupDialog()">新建分组</el-button>
            <el-button type="primary" :disabled="!groups.length" @click="openSettingDialog({})">
              新建监测方案
            </el-button>
          </div>
          <p class="empty-foot">关键词监测支持 + 同时包含、| 任一包含、* 邻近检索等表达式</p>
        </el-empty>
      </div>
    </section>

    <el-dialog
      v-model="groupDialog.visible"
      :title="groupDialog.form.id ? '重命名分组' : '新建分组'"
      width="420px"
      append-to-body
      destroy-on-close
    >
      <el-form label-position="top" @submit.prevent="submitGroup">
        <el-form-item label="分组名称" required>
          <el-input
            v-model="groupDialog.form.name"
            maxlength="50"
            show-word-limit
            placeholder="如：品牌舆情 / 竞品动态"
            @keyup.enter="submitGroup"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="groupDialog.visible = false">取消</el-button>
        <el-button type="primary" :loading="groupDialog.saving" @click="submitGroup">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog
      v-model="settingDialog.visible"
      :title="settingDialog.form.id ? '编辑监测方案' : '新建监测方案'"
      width="640px"
      append-to-body
      destroy-on-close
    >
      <el-form label-position="top" @submit.prevent="submitSetting">
        <div class="dialog-grid">
          <el-form-item label="方案名称" required>
            <el-input
              v-model="settingDialog.form.name"
              maxlength="30"
              show-word-limit
              placeholder="如：品牌负面监测"
            />
          </el-form-item>
          <el-form-item label="所属分组" required>
            <el-select v-model="settingDialog.form.groupId" placeholder="选择分组">
              <el-option
                v-for="group in groups"
                :key="group.id"
                :label="group.name"
                :value="group.id"
              />
            </el-select>
          </el-form-item>
        </div>
        <el-form-item label="监测关键词" required>
          <el-input
            v-model="settingDialog.form.keywords"
            type="textarea"
            :rows="3"
            maxlength="2000"
            show-word-limit
            placeholder="例如：深圳+(男生|男性) 表示同时包含深圳，并包含男生或男性"
          />
        </el-form-item>
        <div class="syntax-tips">
          <span class="syntax-title">表达式</span>
          <span
            v-for="tip in KEYWORD_SYNTAX_TIPS"
            :key="tip.symbol"
            class="syntax-chip"
          >
            <b>{{ tip.symbol }}</b>{{ tip.label }}
            <em>{{ tip.example }}</em>
          </span>
        </div>
        <el-form-item label="排除关键词" class="exclude-item">
          <el-input
            v-model="settingDialog.form.excludeKeywords"
            type="textarea"
            :rows="2"
            maxlength="2000"
            show-word-limit
            placeholder="命中的信息将被排除，可留空"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="settingDialog.visible = false">取消</el-button>
        <el-button type="primary" :loading="settingDialog.saving" @click="submitSetting">
          保存方案
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { InfoFilled } from '@element-plus/icons-vue'
import MonitorSidebar from '../components/MonitorSidebar.vue'
import UnitHeader from '../components/UnitHeader.vue'
import UnitFilterBar from '../components/UnitFilterBar.vue'
import ArticleFeed from '../components/ArticleFeed.vue'
import StatsPanel from '../components/StatsPanel.vue'
import {
  deleteMonitorGroup,
  deleteMonitorSetting,
  fetchMonitorTree,
  saveMonitorGroup,
  saveMonitorSetting,
  searchArticles,
  siteCountStats
} from '../api'
import { KEYWORD_SYNTAX_TIPS } from '../constants'
import {
  parseKeywordTerms
} from '../utils/format'
import {
  buildQueryPayload,
  exportArticleRows,
  fetchAllMatching,
  mergeExpression
} from '../utils/monitor'

const groups = ref([])
const treeLoading = ref(false)
const selectedId = ref(null)

const activeTab = ref('list')
const articles = ref([])
const total = ref(0)
const loading = ref(false)
const dirty = ref(false)
const refreshKey = ref(0)
const siteCounts = ref({})
const siteCountReady = ref(false)
const pagination = reactive({ page: 1, size: 10 })

const filters = reactive({
  timeRange: 'LAST_7_DAYS',
  customRange: [],
  keyword: '',
  excludeKeyword: '',
  sources: [],
  emotions: [],
  riskLevels: [],
  sort: 'createdAt:desc'
})

const applied = reactive({
  timeRange: 'LAST_7_DAYS',
  customRange: [],
  keyword: '',
  excludeKeyword: '',
  sources: [],
  emotions: [],
  riskLevels: [],
  sort: 'createdAt:desc'
})

const groupDialog = reactive({
  visible: false,
  saving: false,
  form: { id: null, name: '' }
})

const settingDialog = reactive({
  visible: false,
  saving: false,
  form: { id: null, groupId: null, name: '', keywords: '', excludeKeywords: '' }
})

const flatSettings = computed(() => groups.value.flatMap((group) => group.children || []))
const selected = computed(() => flatSettings.value.find((item) => item.id === selectedId.value) || null)
const selectedGroup = computed(() => groups.value.find(
  (group) => (group.children || []).some((item) => item.id === selectedId.value)
) || null)
const keywordTerms = computed(() => parseKeywordTerms(selected.value?.keywords || ''))
const highlightTerms = computed(() => [
  ...keywordTerms.value,
  ...parseKeywordTerms(applied.keyword || '')
])

const statsQuery = computed(() => (
  selected.value ? buildQueryPayload(effectiveQuery()) : null
))

function unitMeta(item) {
  const terms = parseKeywordTerms(item.keywords || '')
  if (!terms.length) return '未设置关键词'
  const text = terms.slice(0, 3).join(' · ')
  return terms.length > 3 ? `${text} 等 ${terms.length} 个词` : text
}

function effectiveQuery() {
  return {
    ...applied,
    customRange: [...applied.customRange],
    keywords: mergeExpression(selected.value?.keywords, applied.keyword, '+'),
    excludeKeywords: mergeExpression(selected.value?.excludeKeywords, applied.excludeKeyword, '|')
  }
}

function snapshotApplied() {
  applied.timeRange = filters.timeRange
  applied.customRange = [...filters.customRange]
  applied.keyword = filters.keyword
  applied.excludeKeyword = filters.excludeKeyword
  applied.sources = [...filters.sources]
  applied.emotions = [...filters.emotions]
  applied.riskLevels = [...filters.riskLevels]
  applied.sort = filters.sort
}

function resetFilters() {
  filters.timeRange = 'LAST_7_DAYS'
  filters.customRange = []
  filters.keyword = ''
  filters.excludeKeyword = ''
  filters.sources = []
  filters.emotions = []
  filters.riskLevels = []
  filters.sort = 'createdAt:desc'
}

function validateRange() {
  if (filters.timeRange === 'CUSTOM' && filters.customRange?.length !== 2) {
    ElMessage.warning('请选择完整的自定义时间范围')
    return false
  }
  return true
}

let siteCountRequestId = 0

async function fetchSiteCounts() {
  if (!selected.value) return
  const requestId = ++siteCountRequestId
  const payload = buildQueryPayload(effectiveQuery())
  delete payload.sources
  delete payload.sortBy
  delete payload.sortOrder
  try {
    const data = await siteCountStats(payload)
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
  if (!selected.value) return
  if (applied.timeRange === 'CUSTOM' && applied.customRange?.length !== 2) {
    ElMessage.warning('请选择完整的自定义时间范围')
    return
  }
  loading.value = true
  fetchSiteCounts()
  try {
    const payload = {
      ...buildQueryPayload(effectiveQuery()),
      page: pagination.page,
      size: pagination.size
    }
    const data = await searchArticles(payload)
    articles.value = data?.articleList || []
    total.value = Number(data?.total || 0)
    dirty.value = false
  } catch (error) {
    articles.value = []
    total.value = 0
    ElMessage.error(error.message)
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  if (!validateRange()) return
  snapshotApplied()
  pagination.page = 1
  fetchList()
}

function handleReset() {
  resetFilters()
  handleSearch()
}

function handleFilterChange() {
  dirty.value = true
}

function selectUnit(item) {
  if (!item || item.id === selectedId.value) return
  selectedId.value = item.id
  activeTab.value = 'list'
  resetFilters()
  snapshotApplied()
  pagination.page = 1
  fetchList()
}

function switchTab(tab) {
  activeTab.value = tab
}

function refreshCurrent() {
  if (activeTab.value === 'stats') {
    refreshKey.value += 1
    return
  }
  fetchList()
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

async function exportList() {
  if (!selected.value) return
  loading.value = true
  try {
    const payload = buildQueryPayload(effectiveQuery())
    const { rows } = await fetchAllMatching(payload, { maxPages: 5, pageSize: 100, maxRows: 500 })
    exportArticleRows(rows, `监测单元_${selected.value.name}`)
    ElMessage.success(`已导出 ${rows.length} 条信息`)
  } catch (error) {
    ElMessage.error(error.message || '导出失败')
  } finally {
    loading.value = false
  }
}

async function loadTree() {
  treeLoading.value = true
  try {
    const data = await fetchMonitorTree()
    groups.value = (data || []).map((group) => ({
      ...group,
      children: group.settings || []
    }))
    if (!selected.value) {
      const first = flatSettings.value[0]
      if (first) {
        selectedId.value = first.id
        resetFilters()
        snapshotApplied()
        pagination.page = 1
        await fetchList()
      } else {
        selectedId.value = null
        articles.value = []
        total.value = 0
      }
    }
  } catch (error) {
    ElMessage.error(error.message)
  } finally {
    treeLoading.value = false
  }
}

function openGroupDialog(group = null) {
  groupDialog.form.id = group?.id ?? null
  groupDialog.form.name = group?.name || ''
  groupDialog.visible = true
}

async function submitGroup() {
  const name = groupDialog.form.name.trim()
  if (!name) {
    ElMessage.warning('请输入分组名称')
    return
  }
  groupDialog.saving = true
  try {
    await saveMonitorGroup({ id: groupDialog.form.id ?? undefined, name })
    groupDialog.visible = false
    ElMessage.success(groupDialog.form.id ? '分组名称已更新' : '分组创建成功')
    await loadTree()
  } catch (error) {
    ElMessage.error(error.message)
  } finally {
    groupDialog.saving = false
  }
}

function openSettingDialog({ group = null, setting = null } = {}) {
  if (!groups.value.length) {
    ElMessage.warning('请先创建监测分组')
    openGroupDialog()
    return
  }
  const form = settingDialog.form
  form.id = setting?.id ?? null
  form.groupId = setting?.groupId ?? group?.id ?? selectedGroup.value?.id ?? groups.value[0]?.id ?? null
  form.name = setting?.name || ''
  form.keywords = setting?.keywords || ''
  form.excludeKeywords = setting?.excludeKeywords || ''
  settingDialog.visible = true
}

async function submitSetting() {
  const form = settingDialog.form
  if (!form.groupId) {
    ElMessage.warning('请选择所属分组')
    return
  }
  if (!form.name.trim()) {
    ElMessage.warning('请输入方案名称')
    return
  }
  if (!form.keywords.trim()) {
    ElMessage.warning('请输入监测关键词')
    return
  }
  settingDialog.saving = true
  try {
    const saved = await saveMonitorSetting({
      id: form.id ?? undefined,
      groupId: form.groupId,
      name: form.name.trim(),
      keywords: form.keywords.trim(),
      excludeKeywords: form.excludeKeywords.trim() || null
    })
    settingDialog.visible = false
    ElMessage.success(form.id ? '监测方案已更新' : '监测方案创建成功')
    await loadTree()
    if (saved?.id) {
      selectedId.value = saved.id
      activeTab.value = 'list'
      resetFilters()
      snapshotApplied()
      pagination.page = 1
      await fetchList()
    }
  } catch (error) {
    ElMessage.error(error.message)
  } finally {
    settingDialog.saving = false
  }
}

async function removeSetting(item) {
  if (!item) return
  try {
    await ElMessageBox.confirm(
      `确认删除监测方案「${item.name}」吗？删除后不可恢复。`,
      '删除监测方案',
      { type: 'warning', confirmButtonText: '删除', cancelButtonText: '取消' }
    )
  } catch {
    return
  }
  try {
    await deleteMonitorSetting(item.id)
    if (selectedId.value === item.id) selectedId.value = null
    ElMessage.success('监测方案已删除')
    await loadTree()
  } catch (error) {
    ElMessage.error(error.message)
  }
}

async function removeGroup(group) {
  try {
    await ElMessageBox.confirm(
      `确认删除分组「${group.name}」吗？仅当分组下无监测方案时可删除。`,
      '删除分组',
      { type: 'warning', confirmButtonText: '删除', cancelButtonText: '取消' }
    )
  } catch {
    return
  }
  try {
    await deleteMonitorGroup(group.id)
    ElMessage.success('分组已删除')
    await loadTree()
  } catch (error) {
    ElMessage.error(error.message)
  }
}

onMounted(loadTree)
</script>

<style scoped>
.monitor-page {
  display: flex;
  height: 100%;
  background: var(--bg);
}

.monitor-page :deep(.monitor-sidebar) {
  width: 175px;
}

.workbench {
  display: flex;
  flex: 1;
  flex-direction: column;
  min-width: 0;
  min-height: 0;
}

.workbench-body {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  padding-bottom: 26px;
}

.list-wrap {
  padding: 14px 20px 0;
}

.feed-card {
  padding: 2px 18px 6px;
  background: #fff;
  border: 1px solid var(--border);
  border-radius: var(--radius);
  box-shadow: var(--shadow-sm);
}

.dirty-bar {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 12px;
  margin-bottom: 10px;
  font-size: 11.5px;
  color: #8a5a12;
  background: #fff8e8;
  border: 1px solid #f5e2b4;
  border-radius: 8px;
}

.empty-workbench {
  display: flex;
  flex: 1;
  align-items: center;
  justify-content: center;
  padding: 40px;
}

.empty-actions {
  display: flex;
  gap: 10px;
  justify-content: center;
}

.empty-foot {
  margin: 14px 0 0;
  font-size: 11.5px;
  color: var(--text-muted);
}

.dialog-grid {
  display: grid;
  grid-template-columns: 1.4fr 1fr;
  gap: 16px;
}

.syntax-tips {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
  padding: 0 0 16px;
  margin-top: -8px;
}

.syntax-title {
  font-size: 11.5px;
  color: var(--text-muted);
}

.syntax-chip {
  padding: 3px 8px;
  font-size: 11px;
  color: #56657c;
  background: #f5f8fd;
  border: 1px solid #e6edf8;
  border-radius: 999px;
}

.syntax-chip b {
  margin-right: 3px;
  color: var(--brand);
}

.syntax-chip em {
  margin-left: 4px;
  font-style: normal;
  color: #9aa6b8;
}

.exclude-item {
  margin-bottom: 4px;
}

@media (max-width: 1360px) {
  .dialog-grid {
    grid-template-columns: 1fr;
    gap: 0;
  }
}
</style>