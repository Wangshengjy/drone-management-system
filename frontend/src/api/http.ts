import axios, { type AxiosRequestConfig } from 'axios'
import { ElMessage } from 'element-plus'

export type ApiHttp = {
  get<T = any>(url: string, config?: AxiosRequestConfig): Promise<T>
  post<T = any>(url: string, data?: unknown, config?: AxiosRequestConfig): Promise<T>
  put<T = any>(url: string, data?: unknown, config?: AxiosRequestConfig): Promise<T>
  delete<T = any>(url: string, config?: AxiosRequestConfig): Promise<T>
}

const client = axios.create({
  baseURL: '/',
  timeout: 30000,
})

let lastErrorMessage = ''
let lastErrorAt = 0

function notifyError(message: string) {
  const now = Date.now()
  if (message === lastErrorMessage && now - lastErrorAt < 3000) {
    return
  }
  lastErrorMessage = message
  lastErrorAt = now
  ElMessage.error(message)
}

function normalizeErrorMessage(err: any) {
  const status = err?.response?.status
  const raw = String(err?.response?.data?.message || err?.message || '').trim()
  if (!err?.response || status >= 502 || /network error|failed to fetch|econnrefused|timeout/i.test(raw)) {
    return '后端服务暂时不可用，请确认 8080 服务和数据库已启动'
  }
  return raw || '网络错误'
}

client.interceptors.request.use((config) => {
  const token = localStorage.getItem('token')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

client.interceptors.response.use(
  (res) => {
    const body = res.data
    if (body && typeof body.code === 'number') {
      if (body.code !== 0) {
        notifyError(body.message || '请求失败')
        return Promise.reject(body)
      }
      return body.data
    }
    return res.data
  },
  (err) => {
    notifyError(normalizeErrorMessage(err))
    return Promise.reject(err)
  },
)

const http = client as ApiHttp
export default http
