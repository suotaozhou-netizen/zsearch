<template>
  <div class="filter-bar">
    <div class="filter-main">
      <span class="filter-label">监测时间</span>
      <el-radio-group
        :model-value="filters.timeRange"
        size="small"
        @update:model-value="handleTimeChange"
      >
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
        :model-value="filters.customRange"
        type="datetimerange"
        size="small"
        value-format="YYYY-MM-DD HH:mm:ss"
        range-separator="至"
        start-placeholder="开始时间"
        end-placeholder="结束时间"
        class="custom-time"
        @update:model-value="handleCustomRange"
      />

      <div class="filter-spacer"></div>

      <el-select
        v-model="filters.sort"
        size="small"
        class="sort-select"
        @change="emit('change')"
      >
        <template #prefix>
          <el-icon><Sort /></el-icon>
        </template>
        <el-option
          v-for="item in SORT_OPTIONS"
          :key="item.value"
          :label="item.label"
          :value="item.value"
        />
      </el-select>

      <el-button
        size="small"
        :icon="Filter"
        class="toggle-btn"
        @click="expanded = !expanded"
      >
        {{ expanded ? '收起筛选' : '更多筛选' }}
        <el-icon class="toggle-arrow" :class="{ up: expanded }"><ArrowDown /></el-icon>
      </el-button>
      <el-button type="primary" size="small" :icon="Search" @click="emit('search')">
        查询
      </el-button>
    </div>

    <div v-if="showSources" class="media-row">
      <span class="filter-label">媒体类型</span>
      <el-checkbox-group
        v-model="filters.sources"
        class="media-group"
        @change="emit('change')"
      >
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

    <div v-show="expanded" class="filter-advanced">
      <div v-if="showKeyword" class="advanced-item">
        <span class="filter-label">方案内筛选</span>
        <el-input
          v-model="filters.keyword"
          size="small"
          placeholder="在当前方案结果中二次筛选"
          clearable
          class="advanced-input"
          @keyup.enter="emit('search')"
          @change="emit('change')"
        >
          <template #prefix><el-icon><Search /></el-icon></template>
        </el-input>
      </div>

      <div class="advanced-item">
        <span class="filter-label">排除词</span>
        <el-input
          v-model="filters.excludeKeyword"
          size="small"
          placeholder="在结果中排除包含这些词的信息"
          clearable
          class="advanced-input"
          @keyup.enter="emit('search')"
          @change="emit('change')"
        />
      </div>

      <div class="advanced-item">
        <span class="filter-label">情感</span>
        <el-select
          v-model="filters.emotions"
          size="small"
          multiple
          collapse-tags
          collapse-tags-tooltip
          clearable
          placeholder="全部情感"
          class="advanced-select narrow"
          @change="emit('change')"
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
          size="small"
          multiple
          collapse-tags
          collapse-tags-tooltip
          clearable
          placeholder="全部等级"
          class="advanced-select narrow"
          @change="emit('change')"
        >
          <el-option
            v-for="item in RISK_OPTIONS"
            :key="item.value"
            :label="item.label"
            :value="item.value"
          />
        </el-select>
      </div>

      <div class="advanced-actions">
        <el-button size="small" :icon="RefreshLeft" @click="emit('reset')">重置</el-button>
        <el-button type="primary" size="small" @click="emit('search')">应用筛选</el-button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, ref } from 'vue'
import { ArrowDown, Filter, RefreshLeft, Search, Sort } from '@element-plus/icons-vue'
import {
  EMOTION_OPTIONS,
  RISK_OPTIONS,
  SITE_OPTIONS,
  SORT_OPTIONS,
  TIME_RANGE_OPTIONS
} from '../constants'

const props = defineProps({
  filters: { type: Object, required: true },
  showKeyword: { type: Boolean, default: true },
  showSources: { type: Boolean, default: true },
  siteCounts: { type: Object, default: () => ({}) },
  siteCountReady: { type: Boolean, default: false }
})

const emit = defineEmits(['search', 'reset', 'change'])
const expanded = ref(false)
const isCustom = computed(() => props.filters.timeRange === 'CUSTOM')

function handleTimeChange(value) {
  props.filters.timeRange = value
  if (value !== 'CUSTOM') {
    props.filters.customRange = []
  }
  emit('change')
}

function handleCustomRange(value) {
  props.filters.customRange = value || []
  emit('change')
}
</script>

<style scoped>
.filter-bar {
  padding: 14px 20px;
  background: #fff;
  border-bottom: 1px solid var(--border);
}

.filter-main {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 10px;
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

.toggle-btn {
  color: var(--text-secondary);
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

.filter-advanced {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 13px 20px;
  padding: 14px 0 2px;
  margin-top: 13px;
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

.advanced-actions {
  display: flex;
  gap: 8px;
  margin-left: auto;
}
</style>