<template>
  <div class="monitor-page">
    <MonitorSidebar
      title="定向监测"
      subtitle="平台与账号方案 · 分组管理"
      :groups="groups"
      :selected-id="selectedId"
      :loading="treeLoading"
      :create-disabled="!groups.length"
      create-label="新建定向方案"
      search-placeholder="搜索分组、方案或账号"
      empty-text="暂无定向监测方案"
      empty-title="锁定平台与账号，持续获取定向内容"
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
          type-label="定向监测"
          status-text="监测中"
          :keyword-terms="keywordTerms"
          :keyword-expression="selected.keywords || ''"
          :exclude-terms="excludeTerms"
          :exclude-expression="selected.excludeKeywords || ''"
          :platforms="platforms"
          :account-count="accountCount"
          :meta="headerMeta"
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
          :show-sources="false"
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
              <div v-if="accountMode && scanInfo" class="scan-note">
                <el-icon><Aim /></el-icon>
                <span>
                  已按监测账号筛选：扫描最近 {{ formatCount(scanInfo.scanned) }} 条平台匹配信息，命中 {{ formatCount(total) }} 条
                </span>
                <span v-if="scanInfo.scanned >= ACCOUNT_SCAN_LIMIT" class="scan-hint">
                  （单次最多扫描 {{ ACCOUNT_SCAN_LIMIT }} 条）
                </span>
              </div>
              <div class="feed-card">
                <ArticleFeed
                  :articles="displayedArticles"
                  :total="total"
                  :page="pagination.page"
                  :page-size="pagination.size"
                  :loading="loading"
                  :highlight-terms="highlightTerms"
                  :empty-text="emptyText"
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
            note="定向监测统计按监测平台与关键词聚合，账号级筛选暂不参与统计。"
          />
        </div>
      </template>

      <div v-else class="empty-workbench">
        <el-empty description="选择左侧定向方案查看信息，或先创建一个方案">
          <div class="empty-actions">
            <el-button v-if="!groups.length" @click="openGroupDialog()">新建分组</el-button>
            <el-button type="primary" :disabled="!groups.length" @click="openSettingDialog({})">
              新建定向方案
            </el-button>
          </div>
          <p class="empty-foot">定向监测锁定指定平台与账号，可持续跟踪重点对象动态</p>
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
            placeholder="如：重点账号 / 竞品矩阵"
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
      :title="settingDialog.form.id ? '编辑定向监测方案' : '新建定向监测方案'"
      width="660px"
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
              placeholder="如：重点博主跟踪"
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

        <el-form-item label="监测平台" required>
          <el-select
            v-model="settingDialog.form.siteIds"
            multiple
            collapse-tags
            collapse-tags-tooltip
            placeholder="选择需要监测的平台"
            class="full-select"
          >
            <el-option
              v-for="site in SITE_OPTIONS"
              :key="site.code"
              :label="site.label"
              :value="site.code"
            />
          </el-select>
        </el-form-item>

        <el-form-item label="监测账号">
          <el-select
            v-model="settingDialog.form.uids"
            multiple
            filterable
            allow-create
            default-first-option
            :reserve-keyword="false"
            placeholder="输入账号 ID 后回车，可添加多个；留空表示监测平台全部账号"
            class="full-select"
          />
          <p class="field-tip">账号 ID 用于锁定具体博主或主页，请与平台侧账号 ID 保持一致。</p>
        </el-form-item>

        <el-form-item label="监测关键词（选填）">
          <el-input
            v-model="settingDialog.form.keywords"
            type="textarea"
            :rows="2"
            maxlength="2000"
            show-word-limit
            placeholder="留空表示监测所选平台/账号的全部内容"
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
import { Aim, InfoFilled } from '@element-plus/icons-vue'
import MonitorSidebar from '../components/MonitorSidebar.vue'
import UnitHeader from '../components/UnitHeader.vue'
import UnitFilterBar from '../components/UnitFilterBar.vue'
import ArticleFeed from '../components/ArticleFeed.vue'
import StatsPanel from '../components/StatsPanel.vue'
import {
  deleteSourceMonitorGroup,
  deleteSourceMonitorSetting,
  fetchSourceMonitorTree,
  saveSourceMonitorGroup,
  saveSourceMonitorSetting,
  searchArticles
} from '../api'
import { KEYWORD_SYNTAX_TIPS, SITE_BY_CODE, SITE_OPTIONS } from '../constants'
import {
  formatCount,
  formatDateTime,
  parseKeywordTerms
} from '../utils/format'
import {
  buildQueryPayload,
  exportArticleRows,
  fetchAllMatching,
  mergeExpression
} from '../utils/monitor'

const ACCOUNT_SCAN_LIMIT = 300

const groups = ref([])
const treeLoading = ref(false)
const selectedId = ref(null)

const activeTab = ref('list')
const articles = ref([])
const localRows = ref([])
const scanInfo = ref(null)
const total = ref(0)
const loading = ref(false)
const dirty = ref(false)
const refreshKey = ref(0)
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
  form: {
    id: null,
    groupId: null,
    name: '',
    keywords: '',
    excludeKeywords: '',
    siteIds: [],
    uids: []
  }
})

const flatSettings = computed(() => groups.value.flatMap((group) => group.children || []))
const selected = computed(() => flatSettings.value.find((item) => item.id === selectedId.value) || null)
const selectedGroup = computed(() => groups.value.find(
  (group) => (group.children || []).some((item) => item.id === selectedId.value)
) || null)

const accountCount = computed(() => (selected.value?.uids || []).length)
const accountMode = computed(() => accountCount.value > 0)
const platforms = computed(() => (selected.value?.siteIds || [])
  .map((code) => SITE_BY_CODE[String(code)])
  .filter(Boolean))
const platformLabels = computed(() => platforms.value.map((site) => site.label))
const keywordTerms = computed(() => parseKeywordTerms(selected.value?.keywords || ''))
const excludeTerms = computed(() => parseKeywordTerms(selected.value?.excludeKeywords || ''))
const highlightTerms = computed(() => [
  ...keywordTerms.value,
  ...parseKeywordTerms(applied.keyword || '')
])

const displayedArticles = computed(() => {
  if (!accountMode.value) return articles.value
  const start = (pagination.page - 1) * pagination.size
  return localRows.value.slice(start, start + pagination.size)
})

const emptyText = computed(() => (
  accountMode.value ? '最近扫描范围内未命中监测账号的信息' : '暂无匹配信息'
))

const headerMeta = computed(() => [
  { label: '创建人', value: selected.value?.createUserName || '管理员' },
  { label: '更新时间', value: formatDateTime(selected.value?.updateTime) }
])

const statsQuery = computed(() => (
  selected.value ? buildQueryPayload(effectiveQuery()) : null
))

function unitMeta(item) {
  const labels = (item.siteIds || [])
    .map((code) => SITE_BY_CODE[String(code)]?.label)
    .filter(Boolean)
  const account = (item.uids || []).length
  const head = labels.slice(0, 2).join('/') || '未选平台'
  return account ? `${head} · ${account} 个账号` : `${head} · 全账号`
}

function effectiveQuery() {
  return {
    ...applied,
    customRange: [...applied.customRange],
    sources: [...platformLabels.value],
    keywords: mergeExpression(selected.value?.keywords, applied.keyword, '+'),
    excludeKeywords: mergeExpression(selected.value?.excludeKeywords, applied.excludeKeyword, '|')
  }
}

function snapshotApplied() {
  applied.timeRange = filters.timeRange
  applied.customRange = [...filters.customRange]
  applied.keyword = filters.keyword
  applied.excludeKeyword = filters.excludeKeyword
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

async function fetchList() {
  if (!selected.value) return
  if (applied.timeRange === 'CUSTOM' && applied.customRange?.length !== 2) {
    ElMessage.warning('请选择完整的自定义时间范围')
    return
  }
  loading.value = true
  try {
    const base = buildQueryPayload(effectiveQuery())
    if (accountMode.value) {
      const uids = new Set((selected.value.uids || []).map((uid) => String(uid)))
      const result = await fetchAllMatching(base, {
        maxPages: Math.ceil(ACCOUNT_SCAN_LIMIT / 100),
        pageSize: 100,
        maxRows: ACCOUNT_SCAN_LIMIT,
        predicate: (row) => uids.has(String(row.userId))
      })
      localRows.value = result.rows
      articles.value = []
      total.value = result.rows.length
      scanInfo.value = { scanned: result.scanned, serverTotal: result.serverTotal }
      const maxPage = Math.max(1, Math.ceil(result.rows.length / pagination.size))
      if (pagination.page > maxPage) pagination.page = maxPage
    } else {
      localRows.value = []
      scanInfo.value = null
      const data = await searchArticles({
        ...base,
        page: pagination.page,
        size: pagination.size
      })
      articles.value = data?.articleList || []
      total.value = Number(data?.total || 0)
    }
    dirty.value = false
  } catch (error) {
    articles.value = []
    localRows.value = []
    total.value = 0
    scanInfo.value = null
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
  if (!accountMode.value) fetchList()
}

function handleSizeChange(value) {
  pagination.size = value
  pagination.page = 1
  if (!accountMode.value) fetchList()
}

async function exportList() {
  if (!selected.value) return
  loading.value = true
  try {
    const rows = accountMode.value
      ? localRows.value
      : (await fetchAllMatching(buildQueryPayload(effectiveQuery()), {
        maxPages: 5,
        pageSize: 100,
        maxRows: 500
      })).rows
    exportArticleRows(rows, `定向监测_${selected.value.name}`)
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
    const data = await fetchSourceMonitorTree()
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
        localRows.value = []
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
    await saveSourceMonitorGroup({ id: groupDialog.form.id ?? undefined, name })
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
  form.siteIds = [...(setting?.siteIds || [])]
  form.uids = [...(setting?.uids || [])]
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
  if (!form.siteIds.length) {
    ElMessage.warning('请至少选择一个监测平台')
    return
  }
  settingDialog.saving = true
  try {
    const saved = await saveSourceMonitorSetting({
      id: form.id ?? undefined,
      groupId: form.groupId,
      name: form.name.trim(),
      keywords: form.keywords.trim() || null,
      excludeKeywords: form.excludeKeywords.trim() || null,
      siteIds: [...form.siteIds],
      uids: form.uids.map((uid) => String(uid).trim()).filter(Boolean)
    })
    settingDialog.visible = false
    ElMessage.success(form.id ? '定向监测方案已更新' : '定向监测方案创建成功')
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
      `确认删除定向监测方案「${item.name}」吗？删除后不可恢复。`,
      '删除定向监测方案',
      { type: 'warning', confirmButtonText: '删除', cancelButtonText: '取消' }
    )
  } catch {
    return
  }
  try {
    await deleteSourceMonitorSetting(item.id)
    if (selectedId.value === item.id) selectedId.value = null
    ElMessage.success('定向监测方案已删除')
    await loadTree()
  } catch (error) {
    ElMessage.error(error.message)
  }
}

async function removeGroup(group) {
  try {
    await ElMessageBox.confirm(
      `确认删除分组「${group.name}」吗？仅当分组下无定向方案时可删除。`,
      '删除分组',
      { type: 'warning', confirmButtonText: '删除', cancelButtonText: '取消' }
    )
  } catch {
    return
  }
  try {
    await deleteSourceMonitorGroup(group.id)
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

.dirty-bar,
.scan-note {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 12px;
  margin-bottom: 10px;
  font-size: 11.5px;
  border-radius: 8px;
}

.dirty-bar {
  color: #8a5a12;
  background: #fff8e8;
  border: 1px solid #f5e2b4;
}

.scan-note {
  color: #0f6e5c;
  background: #effaf6;
  border: 1px solid #cdeee4;
}

.scan-hint {
  color: #6f8d86;
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

.full-select {
  width: 100%;
}

.field-tip {
  margin: 6px 0 0;
  font-size: 11px;
  line-height: 1.5;
  color: var(--text-muted);
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
  color: #0f8a74;
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