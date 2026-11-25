<template>
  <aside class="monitor-sidebar">
    <div class="side-head">
      <div class="side-title-row">
        <div v-if="title">
          <h2 class="side-title">{{ title }}</h2>
          <p v-if="subtitle" class="side-subtitle">{{ subtitle }}</p>
        </div>
        <div class="side-head-actions">
          <span class="total-badge">{{ totalCount }}</span>
          <el-tooltip content="刷新列表" placement="top">
            <el-button
              link
              class="refresh-btn"
              :icon="Refresh"
              :loading="loading"
              @click="emit('refresh')"
            />
          </el-tooltip>
        </div>
      </div>
      <el-input
        v-model="searchText"
        size="small"
        :placeholder="searchPlaceholder"
        clearable
        class="side-search"
      >
        <template #prefix><el-icon><Search /></el-icon></template>
      </el-input>
    </div>

    <div v-loading="loading" class="side-tree">
      <div v-for="group in filteredGroups" :key="group.id" class="tree-group">
        <div class="group-row" @click="toggleGroup(group.id)">
          <el-icon class="caret" :class="{ open: showChildren(group) }">
            <ArrowRight />
          </el-icon>
          <el-icon class="folder">
            <FolderOpened v-if="showChildren(group)" />
            <Folder v-else />
          </el-icon>
          <span class="group-name one-line">{{ group.name }}</span>
          <span class="group-count">{{ (group.children || []).length }}</span>
          <div v-if="groupCrud" class="row-actions" @click.stop>
            <el-tooltip content="新建方案" placement="top">
              <el-button link class="row-action" :icon="Plus" @click="emit('create-setting', group)" />
            </el-tooltip>
            <el-dropdown trigger="click" @command="(command) => handleGroupCommand(command, group)">
              <el-button link class="row-action" :icon="MoreFilled" />
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item command="rename" :icon="Edit">重命名分组</el-dropdown-item>
                  <el-dropdown-item command="delete" :icon="Delete" divided>删除分组</el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </div>
        </div>

        <div v-show="showChildren(group)" class="group-children">
          <div
            v-for="item in group.children"
            :key="item.id"
            class="unit-row"
            :class="{ active: item.id === selectedId }"
            @click="emit('select', item)"
          >
            <span class="unit-dot"></span>
            <div class="unit-copy">
              <span class="unit-name one-line">{{ item.name }}</span>
              <span class="unit-meta one-line">
                <slot name="item-meta" :item="item" />
              </span>
            </div>
            <div class="row-actions" @click.stop>
              <el-dropdown trigger="click" @command="(command) => handleItemCommand(command, item)">
                <el-button link class="row-action" :icon="MoreFilled" />
                <template #dropdown>
                  <el-dropdown-menu>
                    <el-dropdown-item command="edit" :icon="Edit">编辑方案</el-dropdown-item>
                    <el-dropdown-item command="delete" :icon="Delete" divided>删除方案</el-dropdown-item>
                  </el-dropdown-menu>
                </template>
              </el-dropdown>
            </div>
          </div>
          <div v-if="!group.children?.length" class="group-empty">该分组下暂无方案</div>
        </div>
      </div>

      <el-empty
        v-if="!loading && !filteredGroups.length"
        :description="emptyText"
        :image-size="72"
      >
        <p class="empty-hint">{{ emptyTitle }}</p>
      </el-empty>
    </div>

    <div class="side-foot">
      <el-button
        type="primary"
        class="create-primary"
        :icon="Plus"
        :disabled="createDisabled"
        @click="emit('create-setting')"
      >
        {{ createLabel }}
      </el-button>
      <el-button v-if="groupCrud" class="create-secondary" @click="emit('create-group')">
        新建分组
      </el-button>
    </div>
  </aside>
</template>

<script setup>
import { computed, reactive, ref, watch } from 'vue'
import {
  ArrowRight,
  Delete,
  Edit,
  Folder,
  FolderOpened,
  MoreFilled,
  Plus,
  Refresh,
  Search
} from '@element-plus/icons-vue'

const props = defineProps({
  title: { type: String, default: '' },
  subtitle: { type: String, default: '' },
  groups: { type: Array, default: () => [] },
  selectedId: { type: [Number, String], default: null },
  loading: { type: Boolean, default: false },
  createLabel: { type: String, default: '新建方案' },
  createDisabled: { type: Boolean, default: false },
  groupCrud: { type: Boolean, default: true },
  searchPlaceholder: { type: String, default: '搜索分组或方案' },
  emptyText: { type: String, default: '暂无监测方案' },
  emptyTitle: { type: String, default: '创建分组后即可添加监测方案' }
})

const emit = defineEmits([
  'select',
  'create-setting',
  'create-group',
  'edit-group',
  'delete-group',
  'edit-setting',
  'delete-setting',
  'refresh'
])

const searchText = ref('')
const expanded = reactive(new Set())

const totalCount = computed(() => props.groups.reduce(
  (sum, group) => sum + (group.children?.length || 0),
  0
))

const keyword = computed(() => searchText.value.trim().toLowerCase())

const filteredGroups = computed(() => {
  if (!keyword.value) return props.groups
  return props.groups
    .map((group) => {
      const groupMatched = String(group.name || '').toLowerCase().includes(keyword.value)
      const children = (group.children || []).filter((item) => itemMatched(item))
      if (groupMatched) return group
      if (children.length) return { ...group, children }
      return null
    })
    .filter(Boolean)
})

function itemMatched(item) {
  const fields = [item.name, item.keywords, item.excludeKeywords, ...(item.uids || [])]
  return fields
    .filter(Boolean)
    .some((value) => String(value).toLowerCase().includes(keyword.value))
}

function showChildren(group) {
  return keyword.value ? true : expanded.has(group.id)
}

function toggleGroup(id) {
  if (keyword.value) return
  if (expanded.has(id)) {
    expanded.delete(id)
  } else {
    expanded.add(id)
  }
}

function handleGroupCommand(command, group) {
  if (command === 'rename') emit('edit-group', group)
  if (command === 'delete') emit('delete-group', group)
}

function handleItemCommand(command, item) {
  if (command === 'edit') emit('edit-setting', item)
  if (command === 'delete') emit('delete-setting', item)
}

watch(
  () => props.groups,
  (groups) => {
    groups.forEach((group) => expanded.add(group.id))
  },
  { immediate: true, deep: true }
)

watch(
  () => props.selectedId,
  (id) => {
    if (id === null || id === undefined) return
    const group = props.groups.find((item) => (item.children || []).some((child) => child.id === id))
    if (group) expanded.add(group.id)
  },
  { immediate: true }
)
</script>

<style scoped>
.monitor-sidebar {
  display: flex;
  flex-direction: column;
  width: 292px;
  flex-shrink: 0;
  background: #fff;
  border-right: 1px solid var(--border);
}

.side-head {
  padding: 18px 16px 12px;
  border-bottom: 1px solid #f0f4fa;
}

.side-title-row {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 10px;
}

.side-title {
  margin: 0;
  font-size: 15px;
  font-weight: 600;
  color: #1d2b42;
}

.side-subtitle {
  margin: 4px 0 0;
  font-size: 11px;
  color: var(--text-muted);
}

.side-head-actions {
  display: flex;
  align-items: center;
  gap: 4px;
}

.total-badge {
  min-width: 24px;
  padding: 1px 7px;
  font-size: 11px;
  color: var(--brand);
  text-align: center;
  background: var(--brand-soft);
  border-radius: 999px;
}

.refresh-btn {
  color: var(--text-muted);
}

.side-search {
  margin-top: 12px;
}

.side-tree {
  flex: 1;
  min-height: 0;
  padding: 10px 10px 14px;
  overflow-y: auto;
}

.tree-group {
  margin-bottom: 3px;
}

.group-row {
  display: flex;
  align-items: center;
  gap: 6px;
  height: 36px;
  padding: 0 8px;
  color: var(--text-secondary);
  border-radius: 7px;
  cursor: pointer;
  user-select: none;
}

.group-row:hover {
  background: #f7f9fd;
}

.caret {
  font-size: 10px;
  color: #a6b2c3;
  transition: transform 0.16s ease;
}

.caret.open {
  transform: rotate(90deg);
}

.folder {
  font-size: 15px;
  color: #8fa4c7;
}

.group-name {
  flex: 1;
  font-size: 13px;
  font-weight: 600;
  color: #3b4b62;
}

.group-count {
  min-width: 18px;
  font-size: 11px;
  color: #9ba8ba;
  text-align: right;
}

.row-actions {
  display: flex;
  align-items: center;
  gap: 0;
  opacity: 0;
  transition: opacity 0.15s ease;
}

.group-row:hover .row-actions,
.unit-row:hover .row-actions,
.unit-row.active .row-actions {
  opacity: 1;
}

.row-action {
  width: 22px;
  height: 22px;
  padding: 0;
  color: #8b99ad;
}

.row-action:hover {
  color: var(--brand);
}

.group-children {
  padding-left: 14px;
}

.unit-row {
  position: relative;
  display: flex;
  align-items: center;
  gap: 8px;
  min-height: 46px;
  padding: 6px 8px 6px 12px;
  margin: 2px 0;
  border-radius: 8px;
  cursor: pointer;
  transition: background 0.16s ease;
}

.unit-row:hover {
  background: #f7f9fd;
}

.unit-row.active {
  background: linear-gradient(90deg, #eef4ff 0%, #f7faff 100%);
}

.unit-row.active::before {
  position: absolute;
  top: 50%;
  left: 0;
  width: 3px;
  height: 18px;
  content: "";
  background: var(--brand);
  border-radius: 0 3px 3px 0;
  transform: translateY(-50%);
}

.unit-dot {
  width: 6px;
  height: 6px;
  flex-shrink: 0;
  background: #b9c5d6;
  border-radius: 50%;
}

.unit-row.active .unit-dot {
  background: var(--brand);
  box-shadow: 0 0 0 3px rgba(37, 99, 235, 0.12);
}

.unit-copy {
  display: flex;
  flex: 1;
  flex-direction: column;
  min-width: 0;
  line-height: 1.35;
}

.unit-name {
  font-size: 13px;
  color: #34445c;
}

.unit-row.active .unit-name {
  font-weight: 600;
  color: var(--brand);
}

.unit-meta {
  margin-top: 2px;
  font-size: 10.5px;
  color: #9aa7b9;
}

.group-empty {
  padding: 7px 0 7px 32px;
  font-size: 11px;
  color: #aab4c4;
}

.empty-hint {
  margin: 2px 0 0;
  font-size: 11px;
  color: var(--text-muted);
  text-align: center;
}

.side-foot {
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding: 12px 16px 16px;
  border-top: 1px solid #f0f4fa;
}

.create-primary {
  width: 100%;
}

.create-secondary {
  width: 100%;
  margin-left: 0;
}
</style>