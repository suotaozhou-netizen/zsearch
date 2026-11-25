import {
  EMOTION_BY_VALUE,
  RISK_BY_VALUE,
  SITE_BY_CODE,
  SITE_OPTIONS
} from '../constants'

const DEFAULT_SITE = {
  code: 0,
  label: '其他',
  short: '其',
  color: '#8a97ab',
  bg: 'rgba(138, 151, 171, 0.1)'
}

export function parseDate(value) {
  if (!value) return null
  if (value instanceof Date) return Number.isNaN(value.getTime()) ? null : value
  let text = String(value).trim()
  if (!text) return null
  text = text.replace(/(\.\d{3})\d+/, '$1')
  if (/^\d{4}-\d{2}-\d{2}\s+\d{2}:\d{2}/.test(text)) {
    text = text.replace(/\s+/, 'T')
  }
  const date = new Date(text)
  return Number.isNaN(date.getTime()) ? null : date
}

function pad(value) {
  return String(value).padStart(2, '0')
}

export function formatDateTime(value) {
  const date = parseDate(value)
  if (!date) return '-'
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}`
}

export function formatFullDateTime(value) {
  const date = parseDate(value)
  if (!date) return '-'
  return `${formatDateTime(date)}:${pad(date.getSeconds())}`
}

export function formatShortDate(value) {
  const date = parseDate(value)
  if (!date) return '-'
  return `${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}`
}

export function formatChartTime(key, interval) {
  const date = parseDate(key)
  if (!date) return key || '-'
  if (interval === 'hour') {
    return `${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(date.getHours())}:00`
  }
  if (interval === 'month') {
    return `${date.getFullYear()}-${pad(date.getMonth() + 1)}`
  }
  return `${pad(date.getMonth() + 1)}-${pad(date.getDate())}`
}

export function formatCount(value) {
  if (value === null || value === undefined || value === '') return '0'
  const number = Number(value)
  if (Number.isNaN(number)) return String(value)
  return number.toLocaleString('zh-CN')
}

export function formatCompactCount(value) {
  if (value === null || value === undefined || value === '') return '0'
  const number = Number(value)
  if (Number.isNaN(number)) return String(value)
  if (number >= 100000000) return `${(number / 100000000).toFixed(2)}亿`
  if (number >= 10000) return `${(number / 10000).toFixed(number >= 100000 ? 0 : 1)}万`
  return number.toLocaleString('zh-CN')
}

export function formatPercent(part, total) {
  if (!total) return '0%'
  return `${((Number(part || 0) / Number(total)) * 100).toFixed(1)}%`
}

export function siteInfo(articleOrId) {
  const siteId = typeof articleOrId === 'object'
    ? articleOrId?.siteId
    : articleOrId
  if (siteId === null || siteId === undefined || siteId === '') return DEFAULT_SITE
  return SITE_BY_CODE[String(siteId)] || DEFAULT_SITE
}

export function siteLabel(article) {
  const site = siteInfo(article)
  return site.label || article?.source || DEFAULT_SITE.label
}

export function platformByLabel(label) {
  if (!label) return null
  const normalized = String(label).trim().toLowerCase()
  return SITE_OPTIONS.find((site) => site.label.toLowerCase() === normalized
    || String(site.code) === normalized) || null
}

export function emotionMeta(value) {
  return EMOTION_BY_VALUE[value] || { value, label: value || '-', type: 'info', color: '#8a97ab' }
}

export function riskMeta(value) {
  return RISK_BY_VALUE[value] || { value, label: value || '-', type: 'info', color: '#8a97ab' }
}

export function parseKeywordTerms(expression) {
  if (!expression) return []
  const normalized = String(expression).replace(/\\(.)/g, '$1')
  return [...new Set(
    normalized
      .split(/[|+*()\s]+/)
      .map((term) => term.trim())
      .filter(Boolean)
  )]
}

export function highlightSegments(text, terms) {
  if (!text) return []
  const source = String(text)
  const keywords = [...new Set((terms || []).filter(Boolean))]
    .sort((a, b) => b.length - a.length)
  if (!keywords.length) return [{ text: source, mark: false }]

  const lowerText = source.toLowerCase()
  const segments = []
  let cursor = 0

  while (cursor < source.length) {
    let matchIndex = -1
    let matchTerm = ''
    for (const term of keywords) {
      const index = lowerText.indexOf(term.toLowerCase(), cursor)
      if (index === -1) continue
      const better = matchIndex === -1
        || index < matchIndex
        || (index === matchIndex && term.length > matchTerm.length)
      if (better) {
        matchIndex = index
        matchTerm = term
      }
    }
    if (matchIndex === -1) {
      segments.push({ text: source.slice(cursor), mark: false })
      break
    }
    if (matchIndex > cursor) {
      segments.push({ text: source.slice(cursor, matchIndex), mark: false })
    }
    segments.push({
      text: source.slice(matchIndex, matchIndex + matchTerm.length),
      mark: true
    })
    cursor = matchIndex + matchTerm.length
  }
  return segments
}

export function clampText(text, maxLength = 620) {
  if (!text) return ''
  const value = String(text)
  return value.length > maxLength ? `${value.slice(0, maxLength)}...` : value
}

export function formatLocalDateTime(date) {
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}:${pad(date.getSeconds())}`
}

export function resolveTimeRange(preset, customRange) {
  const now = new Date()
  if (!preset) return { startTime: null, endTime: null }
  if (preset === 'CUSTOM') {
    return {
      startTime: customRange?.[0] || null,
      endTime: customRange?.[1] || null
    }
  }

  let start = null
  switch (preset) {
    case 'TODAY':
      start = new Date(now.getFullYear(), now.getMonth(), now.getDate())
      break
    case 'LAST_24_HOURS':
      start = new Date(now.getTime() - 24 * 60 * 60 * 1000)
      break
    case 'LAST_3_DAYS':
      start = new Date(now.getTime() - 3 * 24 * 60 * 60 * 1000)
      break
    case 'LAST_7_DAYS':
      start = new Date(now.getTime() - 7 * 24 * 60 * 60 * 1000)
      break
    case 'LAST_1_MONTH':
      start = new Date(now)
      start.setMonth(start.getMonth() - 1)
      break
    case 'LAST_3_MONTHS':
      start = new Date(now)
      start.setMonth(start.getMonth() - 3)
      break
    default:
      return { startTime: null, endTime: null }
  }
  return {
    startTime: formatLocalDateTime(start),
    endTime: formatLocalDateTime(now)
  }
}

export function pickTrendInterval(startTime, endTime) {
  const start = parseDate(startTime)
  const end = parseDate(endTime)
  if (!start || !end) return 'day'
  const days = Math.max(0, (end.getTime() - start.getTime()) / (24 * 60 * 60 * 1000))
  if (days <= 2) return 'hour'
  if (days <= 120) return 'day'
  if (days <= 365) return 'week'
  return 'month'
}

export function toCsv(rows, columns) {
  const escape = (value) => {
    const text = value === null || value === undefined ? '' : String(value)
    return `"${text.replace(/"/g, '""')}"`
  }
  const header = columns.map((column) => escape(column.label)).join(',')
  const body = rows.map((row) => columns
    .map((column) => escape(typeof column.value === 'function' ? column.value(row) : row[column.value]))
    .join(','))
  return `\ufeff${[header, ...body].join('\r\n')}`
}

export function downloadText(filename, content, mime = 'text/csv;charset=utf-8') {
  const blob = new Blob([content], { type: mime })
  downloadBlob(blob, filename)
}

export function downloadBlob(blob, filename) {
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = filename
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
  URL.revokeObjectURL(url)
}

export function formatTodayLabel() {
  const now = new Date()
  const week = ['日', '一', '二', '三', '四', '五', '六'][now.getDay()]
  return `${now.getFullYear()}年${pad(now.getMonth() + 1)}月${pad(now.getDate())}日 周${week}`
}