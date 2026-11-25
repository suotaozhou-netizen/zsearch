<template>
  <div class="article-feed">
    <div v-if="loading" class="feed-skeleton">
      <div v-for="index in 3" :key="index" class="skeleton-card">
        <div class="skeleton-line w-30"></div>
        <div class="skeleton-line w-70 tall"></div>
        <div class="skeleton-line w-90"></div>
        <div class="skeleton-line w-60"></div>
      </div>
    </div>

    <template v-else>
      <article
        v-for="(row, index) in articles"
        :key="row.id || `${row.url}-${index}`"
        class="article-card"
      >
        <div class="article-head">
          <div class="account-wrap">
            <div v-if="showIndex" class="article-index">{{ rowNumber(index) }}</div>
            <div class="avatar" :style="{ background: siteInfo(row).bg, color: siteInfo(row).color }">
              <span class="avatar-letter">{{ siteInfo(row).short }}</span>
              <span
                v-if="siteInfo(row).logo"
                class="avatar-logo"
                :style="siteLogoStyle(siteInfo(row))"
              ></span>
              <img
                v-if="row.avatar"
                :src="row.avatar"
                referrerpolicy="no-referrer"
                alt=""
                @error="hideAvatar"
              />
            </div>
            <div class="account-copy">
              <div class="account-line">
                <el-link
                  v-if="row.homeUrl || row.url"
                  :href="row.homeUrl || row.url"
                  target="_blank"
                  :underline="false"
                  class="account-name"
                >
                  {{ row.name || '未知账号' }}
                </el-link>
                <span v-else class="account-name">{{ row.name || '未知账号' }}</span>
                <span
                  class="platform-chip"
                  :style="{ color: siteInfo(row).color, background: siteInfo(row).bg }"
                >
                  <img
                    v-if="siteInfo(row).logo"
                    class="chip-logo"
                    :src="siteInfo(row).logo"
                    alt=""
                  />
                  {{ siteInfo(row).label }}
                </span>
                <span v-if="row.settingName" class="setting-chip">{{ row.settingName }}</span>
              </div>
              <div class="account-sub">
                <span>{{ formatDateTime(row.createdAt) }}</span>
                <span v-if="row.crawlerTime" class="crawler-time">采集 {{ formatDateTime(row.crawlerTime) }}</span>
              </div>
            </div>
          </div>

          <div class="head-tags">
            <el-tag
              v-if="row.emotion"
              :type="emotionMeta(row.emotion).type"
              effect="light"
              round
              size="small"
            >
              {{ row.emotion }}
            </el-tag>
            <el-tag
              v-if="row.riskLevel"
              :type="riskMeta(row.riskLevel).type"
              effect="plain"
              round
              size="small"
              class="risk-tag"
            >
              {{ row.riskLevel }}
            </el-tag>
          </div>
        </div>

        <div class="article-body">
          <el-link
            v-if="row.url"
            :href="row.url"
            target="_blank"
            :underline="false"
            class="article-title"
          >
            {{ row.title || '无标题' }}
          </el-link>
          <h3 v-else class="article-title">{{ row.title || '无标题' }}</h3>

          <p v-if="articleText(row)" class="article-text">
            <template v-for="(segment, segIndex) in segmentsFor(row)" :key="segIndex">
              <mark v-if="segment.mark" class="kw-mark">{{ segment.text }}</mark>
              <template v-else>{{ segment.text }}</template>
            </template>
          </p>

          <div v-if="articleImages(row).length" class="article-images">
            <div
              v-for="image in articleImages(row)"
              :key="image"
              class="article-image"
              @click="openArticle(row)"
            >
              <img :src="image" referrerpolicy="no-referrer" alt="" @error="handleImageError" />
              <el-icon class="image-placeholder"><Picture /></el-icon>
            </div>
          </div>

          <div class="article-footer">
            <div class="metrics">
              <span class="metric"><el-icon><Star /></el-icon>点赞 {{ formatCompactCount(row.likeCount) }}</span>
              <span class="metric"><el-icon><ChatDotRound /></el-icon>评论 {{ formatCompactCount(row.commentCount) }}</span>
              <span class="metric"><el-icon><Share /></el-icon>转发 {{ formatCompactCount(row.repostCount) }}</span>
              <span class="metric"><el-icon><Collection /></el-icon>收藏 {{ formatCompactCount(row.collectCount) }}</span>
              <span class="metric accent"><el-icon><TrendCharts /></el-icon>互动 {{ formatCompactCount(row.interactionCount) }}</span>
            </div>
            <div class="article-actions">
              <el-button link size="small" :icon="CopyDocument" @click="copyLink(row)">复制链接</el-button>
              <el-button
                v-if="row.url"
                link
                size="small"
                type="primary"
                :icon="Position"
                @click="openArticle(row)"
              >
                查看原文
              </el-button>
            </div>
          </div>
        </div>
      </article>

      <el-empty v-if="!articles.length" :description="emptyText">
        <template v-if="emptyTip" #default>
          <p class="empty-tip">{{ emptyTip }}</p>
        </template>
      </el-empty>
    </template>

    <div v-if="!loading && total > 0" class="pagination-bar">
      <el-pagination
        background
        :current-page="page"
        :page-size="pageSize"
        :page-sizes="pageSizes"
        :total="total"
        layout="total, sizes, prev, pager, next, jumper"
        @current-change="(value) => emit('update:page', value)"
        @size-change="(value) => emit('update:pageSize', value)"
      />
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { ElMessage } from 'element-plus'
import {
  ChatDotRound,
  Collection,
  CopyDocument,
  Picture,
  Position,
  Share,
  Star,
  TrendCharts
} from '@element-plus/icons-vue'
import {
  clampText,
  emotionMeta,
  formatCompactCount,
  formatDateTime,
  highlightSegments,
  parseKeywordTerms,
  riskMeta,
  siteInfo
} from '../utils/format'

const props = defineProps({
  articles: { type: Array, default: () => [] },
  total: { type: Number, default: 0 },
  page: { type: Number, default: 1 },
  pageSize: { type: Number, default: 10 },
  pageSizes: { type: Array, default: () => [10, 20, 50] },
  loading: { type: Boolean, default: false },
  highlightTerms: { type: Array, default: () => [] },
  emptyText: { type: String, default: '暂无信息' },
  emptyTip: { type: String, default: '' },
  showIndex: { type: Boolean, default: false }
})

const emit = defineEmits(['update:page', 'update:pageSize'])

const globalTerms = computed(() => {
  const terms = new Set(props.highlightTerms || [])
  for (const article of props.articles) {
    const highlight = article?.highlight
    if (highlight && !Array.isArray(highlight)) {
      Object.keys(highlight).forEach((key) => terms.add(key))
    }
  }
  return [...terms].filter(Boolean)
})

function rowNumber(index) {
  return (props.page - 1) * props.pageSize + index + 1
}

function articleText(row) {
  const value = row.summary || row.text || row.ocr || row.asr || ''
  return clampText(value, 420)
}

function articleImages(row) {
  if (!Array.isArray(row.imageList)) return []
  return row.imageList.filter((item) => typeof item === 'string' && item).slice(0, 3)
}

function segmentsFor(row) {
  const terms = globalTerms.value.length
    ? globalTerms.value
    : parseKeywordTerms(row.keywords || '')
  return highlightSegments(articleText(row), terms)
}

function hideAvatar(event) {
  event.target.style.display = 'none'
}

// 平台 logo 用 CSS mask 着色为品牌色，保证在头像底色上清晰可辨
function siteLogoStyle(site) {
  if (!site?.logo) return {}
  return {
    backgroundColor: site.color,
    WebkitMaskImage: `url(${site.logo})`,
    maskImage: `url(${site.logo})`
  }
}

function handleImageError(event) {
  const img = event.target
  if (!img.dataset.retried && img.src.startsWith('http://')) {
    img.dataset.retried = '1'
    img.src = img.src.replace('http://', 'https://')
    return
  }
  img.style.display = 'none'
}

function openArticle(row) {
  if (row.url) window.open(row.url, '_blank', 'noopener')
}

async function copyLink(row) {
  if (!row.url) {
    ElMessage.warning('该信息没有原文链接')
    return
  }
  try {
    if (navigator.clipboard && window.isSecureContext) {
      await navigator.clipboard.writeText(row.url)
    } else {
      const textarea = document.createElement('textarea')
      textarea.value = row.url
      textarea.style.position = 'fixed'
      textarea.style.opacity = '0'
      document.body.appendChild(textarea)
      textarea.select()
      document.execCommand('copy')
      document.body.removeChild(textarea)
    }
    ElMessage.success('原文链接已复制')
  } catch {
    ElMessage.error('复制失败，请手动复制')
  }
}
</script>

<style scoped>
.article-feed {
  min-height: 200px;
}

.article-card {
  padding: 18px 4px 20px;
  border-bottom: 1px solid #f0f4fa;
  transition: background 0.18s ease;
}

.article-card:last-of-type {
  border-bottom: none;
}

.article-card:hover {
  background: #fbfdff;
}

.article-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
}

.account-wrap {
  display: flex;
  align-items: center;
  gap: 10px;
  min-width: 0;
}

.article-index {
  width: 20px;
  flex-shrink: 0;
  font-size: 13px;
  font-weight: 600;
  color: var(--text-muted);
}

.avatar {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 36px;
  flex-shrink: 0;
  overflow: hidden;
  font-size: 13px;
  font-weight: 700;
  border-radius: 50%;
}

.avatar img {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.avatar-logo {
  position: absolute;
  inset: 7px;
  -webkit-mask-repeat: no-repeat;
  -webkit-mask-position: center;
  -webkit-mask-size: contain;
  mask-repeat: no-repeat;
  mask-position: center;
  mask-size: contain;
}

.account-copy {
  min-width: 0;
}

.account-line {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 7px;
}

.account-name {
  max-width: 220px;
  overflow: hidden;
  font-size: 13px;
  font-weight: 600;
  color: #253650;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.platform-chip,
.setting-chip {
  padding: 1px 7px;
  font-size: 11px;
  line-height: 18px;
  border-radius: 999px;
}

.chip-logo {
  width: 11px;
  height: 11px;
  margin-right: 3px;
  vertical-align: -1px;
  object-fit: contain;
}

.setting-chip {
  color: var(--brand);
  background: var(--brand-soft);
}

.account-sub {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-top: 5px;
  font-size: 11px;
  color: var(--text-muted);
}

.crawler-time {
  position: relative;
  padding-left: 11px;
}

.crawler-time::before {
  position: absolute;
  top: 50%;
  left: 0;
  width: 3px;
  height: 3px;
  content: "";
  background: #c2cbd9;
  border-radius: 50%;
  transform: translateY(-50%);
}

.head-tags {
  display: flex;
  align-items: center;
  gap: 6px;
  flex-shrink: 0;
  padding-top: 2px;
}

.risk-tag {
  font-weight: 600;
}

.article-body {
  padding-left: 46px;
}

.article-title {
  display: inline-block;
  margin-top: 2px;
  font-size: 15.5px;
  font-weight: 600;
  line-height: 24px;
  color: #18263c;
  text-align: left;
}

.article-title:hover {
  color: var(--brand);
}

.article-text {
  display: -webkit-box;
  margin: 7px 0 0;
  overflow: hidden;
  font-size: 13px;
  line-height: 22px;
  color: #5b6b82;
  word-break: break-word;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
}

.article-images {
  display: flex;
  gap: 8px;
  margin-top: 11px;
}

.article-image {
  position: relative;
  width: 104px;
  height: 76px;
  overflow: hidden;
  background: #f5f7fb;
  border: 1px solid #eef2f8;
  border-radius: 7px;
  cursor: pointer;
}

.article-image img {
  position: relative;
  z-index: 1;
  display: block;
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.image-placeholder {
  position: absolute;
  top: 50%;
  left: 50%;
  z-index: 0;
  font-size: 20px;
  color: #c5cfdd;
  transform: translate(-50%, -50%);
}

.article-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-top: 12px;
}

.metrics {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 16px;
  font-size: 12px;
  color: var(--text-muted);
}

.metric {
  display: inline-flex;
  align-items: center;
  gap: 4px;
}

.metric .el-icon {
  font-size: 13px;
}

.metric.accent {
  color: #3d6fd8;
}

.article-actions {
  display: flex;
  align-items: center;
  gap: 4px;
  flex-shrink: 0;
  opacity: 0;
  transition: opacity 0.18s ease;
}

.article-card:hover .article-actions {
  opacity: 1;
}

.pagination-bar {
  display: flex;
  justify-content: flex-end;
  padding: 18px 0 8px;
}

.empty-tip {
  margin: 0;
  font-size: 12px;
  color: var(--text-muted);
}

.feed-skeleton {
  padding: 4px 0;
}

.skeleton-card {
  padding: 18px 0;
  border-bottom: 1px solid #f0f4fa;
}

.skeleton-line {
  height: 12px;
  margin: 10px 0;
  background: linear-gradient(90deg, #f2f5fa 25%, #e9eff8 37%, #f2f5fa 63%);
  background-size: 400% 100%;
  border-radius: 6px;
  animation: skeleton 1.4s ease infinite;
}

.skeleton-line.tall {
  height: 20px;
}

.w-30 {
  width: 30%;
}

.w-60 {
  width: 60%;
}

.w-70 {
  width: 70%;
}

.w-90 {
  width: 90%;
}

@keyframes skeleton {
  0% {
    background-position: 100% 50%;
  }
  100% {
    background-position: 0 50%;
  }
}

@media (max-width: 1440px) {
  .article-actions {
    opacity: 1;
  }
}
</style>