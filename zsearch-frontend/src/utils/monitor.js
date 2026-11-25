import { searchArticles } from '../api'
import { SITE_SOURCE_BY_LABEL } from '../constants'
import {
  downloadText,
  formatDateTime,
  resolveTimeRange,
  siteLabel,
  toCsv
} from './format'

export function mergeExpression(base, extra, joiner) {
  const left = (base || '').trim()
  const right = (extra || '').trim()
  if (!left) return right || null
  if (!right) return left
  return `(${left})${joiner}(${right})`
}

export function buildQueryPayload(query = {}) {
  const { startTime, endTime } = resolveTimeRange(query.timeRange, query.customRange)
  const [sortBy, sortOrder] = (query.sort || 'createdAt:desc').split(':')
  const payload = {
    monitorType: 0,
    searchScope: 'FULL_TEXT',
    sortBy,
    sortOrder
  }
  if (query.keywords) payload.keywords = query.keywords
  if (query.excludeKeywords) payload.excludeKeywords = query.excludeKeywords
  if (startTime) payload.startTime = startTime
  if (endTime) payload.endTime = endTime
  if (query.sources?.length) {
    payload.sources = query.sources.map((label) => SITE_SOURCE_BY_LABEL[label] || label)
  }
  if (query.emotions?.length) payload.emotions = [...query.emotions]
  if (query.riskLevels?.length) payload.riskLevels = [...query.riskLevels]
  return payload
}

export async function fetchAllMatching(payload, {
  maxPages = 3,
  pageSize = 100,
  maxRows = 300,
  predicate
} = {}) {
  const rows = []
  let scanned = 0
  let serverTotal = 0
  let took = 0

  for (let page = 1; page <= maxPages; page += 1) {
    const data = await searchArticles({ ...payload, page, size: pageSize })
    const batch = data?.articleList || []
    serverTotal = Number(data?.total || 0)
    took += Number(data?.took || 0)
    scanned += batch.length
    for (const row of batch) {
      if (rows.length >= maxRows) break
      if (!predicate || predicate(row)) rows.push(row)
    }
    if (!batch.length || scanned >= serverTotal || rows.length >= maxRows) break
  }

  return { rows, scanned, serverTotal, took }
}

const EXPORT_COLUMNS = [
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
]

export function exportArticleRows(rows, prefix) {
  const csv = toCsv(rows, EXPORT_COLUMNS)
  const stamp = formatDateTime(new Date()).replace(/[: ]/g, '-')
  downloadText(`${prefix}_${stamp}.csv`, csv)
}