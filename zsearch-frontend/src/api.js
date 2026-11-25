import axios from 'axios'

const TOKEN_KEY = 'zsearch_token'

const http = axios.create({
  baseURL: '/api',
  timeout: 30000
})

export function getToken() {
  return localStorage.getItem(TOKEN_KEY) || ''
}

export function setToken(token) {
  localStorage.setItem(TOKEN_KEY, token)
}

export function clearToken() {
  localStorage.removeItem(TOKEN_KEY)
}

http.interceptors.request.use((config) => {
  const token = getToken()
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

http.interceptors.response.use(
  (response) => {
    const body = response.data
    if (body && typeof body.code === 'number') {
      if (body.code !== 200) {
        const error = new Error(body.message || '请求失败')
        error.code = body.code
        return Promise.reject(error)
      }
      return body.data
    }
    return body
  },
  (error) => {
    const status = error.response?.status
    const isLoginRequest = String(error.config?.url || '').includes('/auth/login')
    if (status === 401 && !isLoginRequest) {
      clearToken()
      window.dispatchEvent(new CustomEvent('zsearch:unauthorized'))
    }
    if (error.response?.data instanceof Blob) {
      return error.response.data.text()
        .catch(() => '')
        .then((text) => {
          let message = ''
          try {
            const parsed = JSON.parse(text)
            message = parsed.message || parsed.error || ''
          } catch {
            message = text
          }
          const wrapped = new Error(message || '请求失败')
          wrapped.status = status
          wrapped.response = error.response
          return Promise.reject(wrapped)
        })
    }
    const message = error.response?.data?.message
      || error.response?.data?.error
      || error.message
      || '网络异常'
    const wrapped = new Error(message)
    wrapped.status = status
    wrapped.response = error.response
    return Promise.reject(wrapped)
  }
)

function get(url, params) {
  return http.get(url, { params })
}

function post(url, data) {
  return http.post(url, data)
}


function remove(url) {
  return http.delete(url)
}

export function login(payload) {
  return post('/auth/login', payload)
}

export function refreshToken() {
  return post('/auth/refresh')
}

export function fetchCurrentUser() {
  return get('/auth/me')
}

export function logout() {
  return post('/auth/logout')
}

export function searchArticles(payload) {
  return post('/search', payload)
}

export function getArticle(id) {
  return get(`/search/${encodeURIComponent(id)}`)
}

export function overviewStats(payload) {
  return post('/search/overview', payload)
}

export function groupByStats(payload) {
  return post('/search/group-by', payload)
}

export function siteCountStats(payload) {
  return post('/search/site-count', payload)
}

export function authorCountStats(payload) {
  return post('/search/author-count', payload)
}

export function emotionCountStats(payload) {
  return post('/search/emotion-count', payload)
}

export function siteTrendStats(payload) {
  return post('/search/site-trend', payload)
}

export function interactionSumStats(payload) {
  return post('/search/interaction-sum', payload)
}

export function sumStats(payload) {
  return post('/search/sum', payload)
}

export function trendStats(payload) {
  return post('/search/trend', payload)
}

export function fetchMonitorTree() {
  return get('/monitor/tree')
}

export function saveMonitorGroup(payload) {
  return post('/monitor/groups/saveOrUpdate', payload)
}

export function deleteMonitorGroup(id) {
  return post(`/monitor/groups/delete/${id}`)
}

export function saveMonitorSetting(payload) {
  return post('/monitor/settings/saveOrUpdate', payload)
}

export function deleteMonitorSetting(id) {
  return post(`/monitor/settings/delete/${id}`)
}

export function fetchSourceMonitorTree() {
  return get('/source-monitor/tree')
}

export function saveSourceMonitorGroup(payload) {
  return post('/source-monitor/groups/saveOrUpdate', payload)
}

export function deleteSourceMonitorGroup(id) {
  return post(`/source-monitor/groups/delete/${id}`)
}

export function saveSourceMonitorSetting(payload) {
  return post('/source-monitor/settings/saveOrUpdate', payload)
}

export function deleteSourceMonitorSetting(id) {
  return post(`/source-monitor/settings/delete/${id}`)
}

export function fetchExportFields() {
  return get('/export/fields')
}

export function exportRealtime(payload) {
  return http.post('/export/realtime', payload, { responseType: 'blob', timeout: 120000 })
}

export function createExportTask(payload) {
  return post('/export/task', payload)
}

export function fetchExportTask(id) {
  return get(`/export/task/${id}`)
}

export function fetchExportTasks() {
  return get('/export/tasks')
}

export function downloadExportTask(id) {
  return http.get(`/export/download/${id}`, { responseType: 'blob', timeout: 120000 })
}

export default http