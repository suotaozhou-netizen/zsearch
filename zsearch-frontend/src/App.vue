<template>
  <div v-if="!authReady" class="auth-boot">
    <div class="boot-mark">z</div>
    <span>正在载入工作台...</span>
  </div>
  <LoginView v-else-if="!currentUser" @success="handleLoginSuccess" />
  <div v-else class="app-shell">
    <aside class="app-sidebar">
      <div class="brand">
        <div class="brand-mark">z</div>
        <div class="brand-text">
          <strong>zSearch</strong>
          <span>舆情工作台</span>
        </div>
      </div>

      <nav class="side-nav">
        <div class="nav-section">监测工作台</div>
        <button
          v-for="item in primaryNav"
          :key="item.key"
          class="nav-item"
          :class="{ active: activeKey === item.key, disabled: item.disabled }"
          type="button"
          @click="navigate(item)"
        >
          <el-icon class="nav-icon"><component :is="item.icon" /></el-icon>
          <span class="nav-label">{{ item.label }}</span>
          <span v-if="item.disabled" class="nav-soon">即将上线</span>
          <span v-else-if="activeKey === item.key" class="nav-indicator"></span>
        </button>

        <div class="nav-section nav-section-spaced">内容管理</div>
        <button
          v-for="item in secondaryNav"
          :key="item.key"
          class="nav-item"
          :class="{ disabled: item.disabled }"
          type="button"
          @click="navigate(item)"
        >
          <el-icon class="nav-icon"><component :is="item.icon" /></el-icon>
          <span class="nav-label">{{ item.label }}</span>
          <span class="nav-soon">即将上线</span>
        </button>

        <div class="nav-section nav-section-spaced">系统管理</div>
        <button
          v-for="item in systemNav"
          :key="item.key"
          class="nav-item"
          :class="{ active: activeKey === item.key }"
          type="button"
          @click="navigate(item)"
        >
          <el-icon class="nav-icon"><component :is="item.icon" /></el-icon>
          <span class="nav-label">{{ item.label }}</span>
          <span v-if="activeKey === item.key" class="nav-indicator"></span>
        </button>
      </nav>

      <div class="side-footer">
        <div class="user-avatar">{{ userInitial }}</div>
        <div class="user-copy">
          <strong>{{ userDisplayName }}</strong>
          <span>舆情工作台</span>
        </div>
        <div class="footer-actions">
          <el-tooltip content="系统设置" placement="top">
            <el-button link class="footer-action" :icon="Setting" @click="showSoon('系统设置')" />
          </el-tooltip>
          <el-tooltip content="退出登录" placement="top">
            <el-button link class="footer-action" :icon="SwitchButton" @click="handleLogout" />
          </el-tooltip>
        </div>
      </div>
    </aside>

    <div class="app-main">
      <header class="app-topbar">
        <div class="crumb">
          <span class="crumb-root">舆情工作台</span>
          <el-icon class="crumb-sep"><ArrowRight /></el-icon>
          <strong>{{ currentMeta.label }}</strong>
          <span class="crumb-desc">{{ currentMeta.desc }}</span>
        </div>
        <div class="topbar-right">
          <span class="today">{{ today }}</span>
          <span class="topbar-divider"></span>
          <el-tooltip content="帮助与文档" placement="bottom">
            <el-button circle text class="topbar-icon" :icon="Notification" @click="showSoon('帮助中心')" />
          </el-tooltip>
          <div class="top-avatar">{{ userInitial }}</div>
        </div>
      </header>

      <main class="app-content">
        <KeepAlive>
          <component :is="currentView" :key="`${sessionKey}:${activeKey}`" />
        </KeepAlive>
      </main>
    </div>
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import {
  Aim,
  ArrowRight,
  Bell,
  Download,
  Notification,
  Odometer,
  Search,
  Setting,
  Star,
  SwitchButton,
  TrendCharts
} from '@element-plus/icons-vue'
import SearchView from './views/SearchView.vue'
import MonitorUnitsView from './views/MonitorUnitsView.vue'
import TargetedMonitorView from './views/TargetedMonitorView.vue'
import ExportView from './views/ExportView.vue'
import LoginView from './views/LoginView.vue'
import { clearToken, fetchCurrentUser, getToken, logout, refreshToken, setToken } from './api'
import { formatTodayLabel } from './utils/format'

const primaryNav = [
  { key: 'search', label: '信息搜索', desc: '全平台舆情信息检索', icon: Search, view: SearchView },
  { key: 'units', label: '监测单元', desc: '关键词方案监测', icon: Odometer, view: MonitorUnitsView },
  { key: 'targeted', label: '定向监测', desc: '平台与账号定向监测', icon: Aim, view: TargetedMonitorView }
]

const secondaryNav = [
  { key: 'topic', label: '专题跟踪', desc: '专题聚合与追踪', icon: TrendCharts, disabled: true },
  { key: 'alert', label: '预警管理', desc: '风险预警与处置', icon: Bell, disabled: true },
  { key: 'favorite', label: '我的收藏', desc: '收藏内容管理', icon: Star, disabled: true }
]

const systemNav = [
  { key: 'export', label: '导出管理', desc: '实时与离线文章导出', icon: Download, view: ExportView }
]

const allNav = [...primaryNav, ...secondaryNav, ...systemNav]
const viewMap = Object.fromEntries(
  [...primaryNav, ...systemNav].map((item) => [item.key, item.view])
)
const allowedKeys = new Set(Object.keys(viewMap))

function readHash() {
  const key = window.location.hash.replace('#/', '').replace('#', '').trim()
  return allowedKeys.has(key) ? key : 'search'
}

const activeKey = ref(readHash())
const today = formatTodayLabel()
const authReady = ref(false)
const currentUser = ref(null)
const sessionKey = ref(0)

const TOKEN_REFRESH_MIN_INTERVAL = 60 * 1000
let lastRefreshAt = 0
let refreshing = false

async function handlePageActivity() {
  if (refreshing || !currentUser.value || !getToken()) {
    return
  }
  const now = Date.now()
  if (now - lastRefreshAt < TOKEN_REFRESH_MIN_INTERVAL) {
    return
  }
  lastRefreshAt = now
  refreshing = true
  try {
    const data = await refreshToken()
    if (!currentUser.value) {
      return
    }
    if (data?.token) {
      setToken(data.token)
    }
    if (data?.user) {
      currentUser.value = data.user
    }
  } catch (error) {
    // 续签失败由 401 拦截器统一处理
  } finally {
    refreshing = false
  }
}

const userDisplayName = computed(() => {
  const nickname = currentUser.value?.nickname?.trim()
  return nickname || currentUser.value?.username || '用户'
})
const userInitial = computed(() => userDisplayName.value.slice(0, 1))

const currentMeta = computed(() => (
  allNav.find((item) => item.key === activeKey.value) || primaryNav[0]
))
const currentView = computed(() => viewMap[activeKey.value] || SearchView)

function syncTitle() {
  document.title = `${currentMeta.value.label} - zSearch 舆情工作台`
}

function navigate(item) {
  if (item.disabled || !viewMap[item.key]) {
    showSoon(item.label)
    return
  }
  activeKey.value = item.key
  window.location.hash = `#/${item.key}`
  syncTitle()
}

function showSoon(name) {
  ElMessage.info(`${name}功能正在建设中`)
}

function handleHashChange() {
  activeKey.value = readHash()
  syncTitle()
}

async function restoreSession() {
  if (!getToken()) {
    authReady.value = true
    return
  }
  try {
    currentUser.value = await fetchCurrentUser()
  } catch (error) {
    clearToken()
    currentUser.value = null
  } finally {
    authReady.value = true
  }
}

function handleLoginSuccess(data) {
  setToken(data.token)
  currentUser.value = data.user
  sessionKey.value += 1
  authReady.value = true
  lastRefreshAt = Date.now()
}

async function handleLogout() {
  try {
    await logout()
  } catch (error) {
    // 无状态令牌，服务端注销失败不影响本地退出
  }
  clearToken()
  currentUser.value = null
  sessionKey.value += 1
  ElMessage.success('已退出登录')
}

function handleUnauthorized() {
  clearToken()
  currentUser.value = null
}

onMounted(() => {
  window.addEventListener('hashchange', handleHashChange)
  window.addEventListener('zsearch:unauthorized', handleUnauthorized)
  document.addEventListener('click', handlePageActivity, true)
  if (!window.location.hash) {
    window.history.replaceState(null, '', `#/${activeKey.value}`)
  }
  syncTitle()
  restoreSession()
})

onBeforeUnmount(() => {
  window.removeEventListener('hashchange', handleHashChange)
  window.removeEventListener('zsearch:unauthorized', handleUnauthorized)
  document.removeEventListener('click', handlePageActivity, true)
})
</script>

<style scoped>
.app-shell {
  display: flex;
  height: 100%;
  min-width: 1080px;
  background: var(--bg);
}

.app-sidebar {
  display: flex;
  flex-direction: column;
  width: 224px;
  flex-shrink: 0;
  background: #fff;
  border-right: 1px solid var(--border);
}

.brand {
  display: flex;
  align-items: center;
  gap: 12px;
  height: 64px;
  padding: 0 20px;
  border-bottom: 1px solid #f0f4fa;
}

.brand-mark {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 36px;
  color: #fff;
  font-family: Georgia, serif;
  font-size: 24px;
  font-style: italic;
  font-weight: 700;
  background: linear-gradient(135deg, #3182ff 0%, #1d4ed8 100%);
  border-radius: 11px;
  box-shadow: 0 6px 14px rgba(37, 99, 235, 0.28);
}

.brand-text {
  display: flex;
  flex-direction: column;
  line-height: 1.2;
}

.brand-text strong {
  font-size: 17px;
  letter-spacing: 0.4px;
  color: #14233b;
}

.brand-text span {
  margin-top: 3px;
  font-size: 11px;
  color: var(--text-muted);
  letter-spacing: 2px;
}

.side-nav {
  flex: 1;
  padding: 16px 12px;
  overflow-y: auto;
}

.nav-section {
  padding: 4px 10px 8px;
  font-size: 11px;
  color: #a3aec0;
  letter-spacing: 1.4px;
}

.nav-section-spaced {
  margin-top: 18px;
}

.nav-item {
  position: relative;
  display: flex;
  align-items: center;
  gap: 11px;
  width: 100%;
  height: 44px;
  padding: 0 12px;
  margin-bottom: 4px;
  color: var(--text-secondary);
  font-size: 14px;
  text-align: left;
  background: transparent;
  border: 0;
  border-radius: 9px;
  cursor: pointer;
  transition: color 0.18s ease, background 0.18s ease;
}

.nav-item:hover:not(.disabled) {
  color: var(--brand);
  background: #f6f9ff;
}

.nav-item.active {
  color: var(--brand);
  font-weight: 600;
  background: linear-gradient(90deg, #eef4ff 0%, #f8fbff 100%);
}

.nav-item.disabled {
  color: #aab4c4;
  cursor: not-allowed;
}

.nav-icon {
  width: 18px;
  font-size: 18px;
}

.nav-label {
  flex: 1;
}

.nav-soon {
  padding: 2px 6px;
  font-size: 10px;
  color: #9aa6b8;
  background: #f4f6fa;
  border-radius: 999px;
}

.nav-indicator {
  width: 6px;
  height: 6px;
  background: var(--brand);
  border-radius: 50%;
  box-shadow: 0 0 0 4px rgba(37, 99, 235, 0.1);
}

.side-footer {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 14px 16px;
  border-top: 1px solid #f0f4fa;
}

.user-avatar,
.top-avatar {
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-weight: 600;
  background: linear-gradient(135deg, #60a5fa, #2563eb);
  border-radius: 50%;
}

.user-avatar {
  width: 34px;
  height: 34px;
  font-size: 13px;
}

.user-copy {
  display: flex;
  flex: 1;
  flex-direction: column;
  min-width: 0;
  line-height: 1.35;
}

.user-copy strong {
  font-size: 13px;
}

.user-copy span {
  margin-top: 2px;
  font-size: 11px;
  color: var(--text-muted);
}

.footer-action {
  color: var(--text-muted);
}

.footer-actions {
  display: flex;
  align-items: center;
}

.auth-boot {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 16px;
  height: 100%;
  color: var(--text-muted);
  font-size: 13px;
  background:
    radial-gradient(760px 460px at 15% 10%, rgba(37, 99, 235, 0.12), transparent 60%),
    var(--bg);
}

.boot-mark {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 52px;
  height: 52px;
  color: #fff;
  font-family: Georgia, serif;
  font-size: 34px;
  font-style: italic;
  font-weight: 700;
  background: linear-gradient(135deg, #3182ff 0%, #1d4ed8 100%);
  border-radius: 16px;
  box-shadow: 0 14px 30px rgba(37, 99, 235, 0.3);
  animation: boot-breathe 1.6s ease-in-out infinite;
}

@keyframes boot-breathe {
  0%,
  100% {
    transform: translateY(0);
  }
  50% {
    transform: translateY(-4px);
  }
}

.app-main {
  display: flex;
  flex: 1;
  flex-direction: column;
  min-width: 0;
  min-height: 0;
}

.app-topbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 60px;
  flex-shrink: 0;
  padding: 0 26px;
  background: rgba(255, 255, 255, 0.92);
  border-bottom: 1px solid var(--border);
  backdrop-filter: blur(8px);
}

.crumb {
  display: flex;
  align-items: center;
  gap: 9px;
  font-size: 13px;
}

.crumb-root {
  color: var(--text-muted);
}

.crumb-sep {
  font-size: 11px;
  color: #b3bdcc;
}

.crumb strong {
  font-size: 14px;
  color: var(--text);
}

.crumb-desc {
  margin-left: 4px;
  padding-left: 12px;
  font-size: 12px;
  color: var(--text-muted);
  border-left: 1px solid var(--border);
}

.topbar-right {
  display: flex;
  align-items: center;
  gap: 14px;
}

.today {
  font-size: 12px;
  color: var(--text-muted);
}

.topbar-divider {
  width: 1px;
  height: 18px;
  background: var(--border);
}

.topbar-icon {
  color: var(--text-secondary);
}

.top-avatar {
  width: 32px;
  height: 32px;
  font-size: 12px;
}

.app-content {
  flex: 1;
  min-height: 0;
  overflow: hidden;
}
</style>