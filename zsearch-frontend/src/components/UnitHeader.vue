<template>
  <header class="unit-header">
    <div class="unit-head-row">
      <div class="unit-identity">
        <div class="unit-icon" :class="{ targeted: typeLabel === '定向监测' }">
          <el-icon><component :is="typeIcon" /></el-icon>
        </div>
        <div class="unit-copy" :class="{ 'no-crumb': !showCrumb }">
          <div v-if="showCrumb" class="unit-crumb">
            <span>{{ groupName || '未分组' }}</span>
            <el-icon><ArrowRight /></el-icon>
            <span>{{ typeLabel }}</span>
          </div>
          <div class="unit-title-row">
            <h2 class="unit-title">{{ title }}</h2>
            <el-tag v-if="showTypeTag" effect="plain" round size="small" class="type-tag">{{ typeLabel }}</el-tag>
            <span v-if="showStatus" class="run-status"><i></i>{{ statusText }}</span>
          </div>
        </div>
      </div>

      <div class="unit-actions">
        <el-button size="small" :icon="Refresh" :loading="refreshing" @click="emit('refresh')">
          刷新
        </el-button>
        <el-button size="small" :icon="Edit" @click="emit('edit')">编辑</el-button>
        <el-button size="small" :icon="Download" @click="emit('export')">导出</el-button>
        <el-dropdown trigger="click" @command="handleCommand">
          <el-button size="small" :icon="MoreFilled" class="more-btn" />
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="delete" :icon="Delete">删除方案</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </div>
    </div>

    <div v-if="hasMeta" class="unit-meta">
      <div v-if="keywordTerms.length" class="meta-block keyword-block">
        <span class="meta-label" :title="keywordExpression">关键词</span>
        <div class="chip-list">
          <span
            v-for="term in visibleKeywords"
            :key="term"
            class="keyword-chip"
            :title="term"
          >
            {{ term }}
          </span>
          <el-tooltip
            v-if="keywordTerms.length > visibleKeywords.length"
            :content="keywordTerms.join('、')"
            placement="bottom"
          >
            <span class="more-chip">+{{ keywordTerms.length - visibleKeywords.length }}</span>
          </el-tooltip>
        </div>
      </div>
      <div v-else-if="typeLabel === '定向监测'" class="meta-block">
        <span class="meta-label">关键词</span>
        <span class="meta-empty">未设置，监测所选平台与账号的全部内容</span>
      </div>

      <div v-if="excludeTerms.length" class="meta-block">
        <span class="meta-label" :title="excludeExpression">排除词</span>
        <div class="chip-list">
          <span
            v-for="term in excludeTerms.slice(0, 6)"
            :key="term"
            class="exclude-chip"
            :title="term"
          >
            {{ term }}
          </span>
          <span v-if="excludeTerms.length > 6" class="more-chip">+{{ excludeTerms.length - 6 }}</span>
        </div>
      </div>

      <div v-if="platforms.length" class="meta-block">
        <span class="meta-label">监测平台</span>
        <div class="platform-list">
          <span
            v-for="platform in platforms"
            :key="platform.code"
            class="platform-badge"
            :style="{ color: platform.color, background: platform.bg }"
          >
            <i :style="{ background: platform.color }"></i>{{ platform.label }}
          </span>
        </div>
      </div>

      <div v-if="accountCount" class="meta-block">
        <span class="meta-label">监测账号</span>
        <span class="meta-value">{{ accountCount }} 个</span>
      </div>

      <div v-for="item in meta" :key="item.label" class="meta-block subtle">
        <span class="meta-label">{{ item.label }}</span>
        <span class="meta-value">{{ item.value }}</span>
      </div>
    </div>

    <div class="unit-tabs">
      <button
        type="button"
        class="tab-btn"
        :class="{ active: activeTab === 'list' }"
        @click="emit('update:activeTab', 'list')"
      >
        <el-icon><List /></el-icon>
        信息列表
        <span class="tab-count">{{ formatCount(listTotal) }}</span>
      </button>
      <button
        type="button"
        class="tab-btn"
        :class="{ active: activeTab === 'stats' }"
        @click="emit('update:activeTab', 'stats')"
      >
        <el-icon><DataAnalysis /></el-icon>
        统计分析
      </button>
    </div>
  </header>
</template>

<script setup>
import { computed } from 'vue'
import {
  Aim,
  ArrowRight,
  DataAnalysis,
  Delete,
  Download,
  Edit,
  List,
  MoreFilled,
  Odometer,
  Refresh
} from '@element-plus/icons-vue'
import { formatCount } from '../utils/format'

const props = defineProps({
  title: { type: String, default: '' },
  groupName: { type: String, default: '' },
  showCrumb: { type: Boolean, default: true },
  typeLabel: { type: String, default: '关键词监测' },
  showTypeTag: { type: Boolean, default: true },
  statusText: { type: String, default: '运行中' },
  showStatus: { type: Boolean, default: true },
  keywordTerms: { type: Array, default: () => [] },
  keywordExpression: { type: String, default: '' },
  excludeTerms: { type: Array, default: () => [] },
  excludeExpression: { type: String, default: '' },
  platforms: { type: Array, default: () => [] },
  accountCount: { type: Number, default: 0 },
  meta: { type: Array, default: () => [] },
  activeTab: { type: String, default: 'list' },
  listTotal: { type: Number, default: 0 },
  refreshing: { type: Boolean, default: false }
})

const emit = defineEmits(['refresh', 'edit', 'export', 'remove', 'update:activeTab'])

const typeIcon = computed(() => (props.typeLabel === '定向监测' ? Aim : Odometer))
const visibleKeywords = computed(() => props.keywordTerms.slice(0, 8))
const hasMeta = computed(() => props.keywordTerms.length > 0
  || props.excludeTerms.length > 0
  || props.platforms.length > 0
  || props.accountCount > 0
  || props.meta.length > 0)

function handleCommand(command) {
  if (command === 'delete') emit('remove')
}
</script>

<style scoped>
.unit-header {
  flex-shrink: 0;
  padding: 18px 24px 0;
  background: #fff;
  border-bottom: 1px solid var(--border);
}

.unit-head-row {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 18px;
}

.unit-identity {
  display: flex;
  align-items: center;
  gap: 13px;
  min-width: 0;
}

.unit-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 44px;
  height: 44px;
  flex-shrink: 0;
  color: #2563eb;
  font-size: 21px;
  background: linear-gradient(135deg, #eef5ff, #dce9ff);
  border: 1px solid #d5e4ff;
  border-radius: 12px;
}

.unit-icon.targeted {
  color: #0f8a74;
  background: linear-gradient(135deg, #ecfbf7, #d6f5ec);
  border-color: #c9eee5;
}

.unit-copy {
  min-width: 0;
}

.unit-crumb {
  display: flex;
  align-items: center;
  gap: 5px;
  font-size: 11px;
  color: var(--text-muted);
}

.unit-crumb .el-icon {
  font-size: 9px;
}

.unit-title-row {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 9px;
  margin-top: 4px;
}

.unit-copy.no-crumb .unit-title-row {
  margin-top: 0;
}

.unit-title {
  max-width: 520px;
  margin: 0;
  overflow: hidden;
  font-size: 20px;
  font-weight: 600;
  line-height: 26px;
  color: #16243a;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.type-tag {
  color: var(--brand);
  border-color: #cfdefb;
  background: #f7faff;
}

.run-status {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  font-size: 11px;
  color: #14946a;
}

.run-status i {
  width: 6px;
  height: 6px;
  background: #12a56a;
  border-radius: 50%;
  animation: pulse-dot 2s infinite;
}

.unit-actions {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-shrink: 0;
  padding-top: 2px;
}

.more-btn {
  padding: 0 9px;
}

.unit-meta {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px 20px;
  padding: 13px 0 15px;
  margin-top: 12px;
  border-top: 1px dashed #e9eff8;
}

.meta-block {
  display: flex;
  align-items: center;
  gap: 7px;
  min-width: 0;
  font-size: 12px;
}

.meta-label {
  flex-shrink: 0;
  color: var(--text-muted);
}

.meta-value {
  color: #42526b;
}

.meta-empty {
  color: #a0abbb;
}

.chip-list,
.platform-list {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 5px;
}

.keyword-chip,
.exclude-chip,
.more-chip {
  max-width: 150px;
  padding: 2px 8px;
  overflow: hidden;
  font-size: 11px;
  line-height: 18px;
  text-overflow: ellipsis;
  white-space: nowrap;
  border-radius: 999px;
}

.keyword-chip {
  color: #2458c6;
  background: #eef4ff;
}

.exclude-chip {
  color: #a45a2a;
  background: #fff5eb;
}

.more-chip {
  color: var(--text-muted);
  background: #f4f6fa;
}

.platform-badge {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  padding: 2px 8px;
  font-size: 11px;
  line-height: 18px;
  border-radius: 999px;
}

.platform-badge i {
  width: 5px;
  height: 5px;
  border-radius: 50%;
}

.unit-tabs {
  display: flex;
  gap: 26px;
}

.tab-btn {
  position: relative;
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 10px 2px 12px;
  color: var(--text-secondary);
  font-size: 13.5px;
  background: transparent;
  border: 0;
  cursor: pointer;
}

.tab-btn:hover {
  color: var(--brand);
}

.tab-btn.active {
  color: var(--brand);
  font-weight: 600;
}

.tab-btn.active::after {
  position: absolute;
  right: 0;
  bottom: 0;
  left: 0;
  height: 2px;
  content: "";
  background: var(--brand);
  border-radius: 2px 2px 0 0;
}

.tab-count {
  padding: 0 6px;
  font-size: 10.5px;
  font-weight: 500;
  color: var(--text-muted);
  background: #f3f6fb;
  border-radius: 999px;
}

.tab-btn.active .tab-count {
  color: var(--brand);
  background: var(--brand-soft);
}

@media (max-width: 1440px) {
  .unit-title {
    max-width: 360px;
  }
}
</style>