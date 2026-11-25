export const SITE_OPTIONS = [
  { code: 1, label: '抖音', short: '抖', color: '#161823', bg: 'rgba(22, 24, 35, 0.08)', logo: '/sites/douyin.svg' },
  { code: 2, label: '小红书', short: '红', color: '#ff2442', bg: 'rgba(255, 36, 66, 0.08)', logo: '/sites/xiaohongshu.svg' },
  { code: 3, label: 'bilibili', short: 'B', color: '#fb7299', bg: 'rgba(251, 114, 153, 0.1)', logo: '/sites/bilibili.svg' },
  { code: 4, label: '微博', short: '微', color: '#e6162d', bg: 'rgba(230, 22, 45, 0.08)', logo: '/sites/weibo.svg' },
  { code: 5, label: '今日头条', short: '头', color: '#f04142', bg: 'rgba(240, 65, 66, 0.08)', logo: '/sites/toutiao.svg' },
  { code: 6, label: '快手', short: '快', color: '#ff4906', bg: 'rgba(255, 73, 6, 0.08)', logo: '/sites/kuaishou.svg' },
  { code: 7, label: '贴吧', short: '贴', color: '#2932e1', bg: 'rgba(41, 50, 225, 0.08)', logo: '/sites/tieba.svg' },
  { code: 8, label: '知乎', short: '知', color: '#0084ff', bg: 'rgba(0, 132, 255, 0.08)', logo: '/sites/zhihu.svg' }
]

export const SITE_BY_CODE = Object.fromEntries(
  SITE_OPTIONS.map((site) => [String(site.code), site])
)

export const SITE_LABEL_BY_CODE = Object.fromEntries(
  SITE_OPTIONS.map((site) => [String(site.code), site.label])
)

export const SITE_SOURCE_BY_LABEL = {
  抖音: 'douyin',
  小红书: 'xiaohongshu',
  bilibili: 'bilibili',
  微博: 'weibo',
  今日头条: 'toutiao',
  快手: 'kuaishou',
  贴吧: 'tieba',
  知乎: 'zhihu'
}

export const EMOTION_OPTIONS = [
  { value: '正面', label: '正面', type: 'success', color: '#12a56a' },
  { value: '中性', label: '中性', type: 'info', color: '#8a97ab' },
  { value: '负面', label: '负面', type: 'danger', color: '#e5484d' }
]

export const EMOTION_BY_VALUE = Object.fromEntries(
  EMOTION_OPTIONS.map((item) => [item.value, item])
)

export const RISK_OPTIONS = [
  { value: '重点关注', label: '重点关注', type: 'danger', color: '#e5484d' },
  { value: '关注风险', label: '关注风险', type: 'warning', color: '#d97706' }
]

export const RISK_BY_VALUE = Object.fromEntries(
  RISK_OPTIONS.map((item) => [item.value, item])
)

export const TIME_RANGE_OPTIONS = [
  { value: 'TODAY', label: '今日' },
  { value: 'LAST_24_HOURS', label: '近24小时' },
  { value: 'LAST_3_DAYS', label: '近3天' },
  { value: 'LAST_7_DAYS', label: '近7天' },
  { value: 'LAST_1_MONTH', label: '近1个月' },
  { value: 'LAST_3_MONTHS', label: '近3个月' },
  { value: 'CUSTOM', label: '自定义' }
]

export const SORT_OPTIONS = [
  { value: 'createdAt:desc', label: '最新发布' },
  { value: 'createdAt:asc', label: '最早发布' },
  { value: 'interactionCount:desc', label: '互动最多' },
  { value: 'likeCount:desc', label: '点赞最多' },
  { value: 'commentCount:desc', label: '评论最多' },
  { value: 'repostCount:desc', label: '转发最多' },
  { value: 'crawlerTime:desc', label: '最近采集' }
]

export const SEARCH_SCOPE_OPTIONS = [
  { value: 'FULL_TEXT', label: '全文' },
  { value: 'TITLE', label: '标题' },
  { value: 'NAME', label: '作者' },
  { value: 'SOURCE', label: '来源' }
]

export const MONITOR_TYPE = {
  GENERAL: 0,
  KEYWORD: 1,
  SOURCE: 2
}

export const KEYWORD_SYNTAX_TIPS = [
  { symbol: '+', label: '同时包含', example: '深圳+男生' },
  { symbol: '|', label: '任一包含', example: '男生|男性' },
  { symbol: '*', label: '邻近检索', example: '深圳*男生' },
  { symbol: '()', label: '组合条件', example: '深圳+(男生|男性)' }
]

export const DEFAULT_PAGE_SIZES = [10, 20, 50]