/**
 * Axios HTTP 请求封装（前端唯一 HTTP 入口，非流式接口都走这里）
 * ----------------------------------------------------------------
 * 配置：
 *  baseURL: '/api' → Vite dev proxy 转发到后端 8080（见 vite.config.js proxy）
 *  timeout: 180s  → 兼容 AI 照片分析等待（Qwen-VL-Plus 10~30s 响应）
 *  默认 Content-Type: application/json
 *
 * 拦截器链：
 *  ┌────────────────────────────────────┐
 *  │ 1. 请求拦截：localStorage.token   │  → 自动加 Authorization: Bearer <JWT>
 *  ├────────────────────────────────────┤
 *  │ 2. 响应拦截：解包 ApiResponse     │  → return res.data（直接给业务层纯数据）
 *  │     res.code ≠ 200 → ElMessage 弹窗 + Promise.reject 跳转到 catch
 *  ├────────────────────────────────────┤
 *  │ 3. 错误拦截：按 HTTP 状态码分发   │  → 401 清token跳登录 / 403 权限不足 / 500 服务器错
 *  └────────────────────────────────────┘
 *
 * 注：SSE 流式接口（/message/stream 和 /message/image）走原生 XMLHttpRequest，
 *     不经过 axios，见 src/api/chat.js 中的 sendMessageStream / sendMessageWithImage
 */
import axios from 'axios'
import { ElMessage } from 'element-plus'

const request = axios.create({
  baseURL: '/api',
  timeout: 180000,
  headers: {
    'Content-Type': 'application/json'
  }
})

/**
 * 静默请求实例：不挂载任何全局拦截器（不弹错误提示、不自动跳转登录页）
 * 仅供"启动时校验本地 Token 是否有效"这类需要安静失败的场景使用，
 * 业务请求一律走上方带拦截器的 request。
 */
const silentRequest = axios.create({
  baseURL: '/api',
  timeout: 10000,
  headers: {
    'Content-Type': 'application/json'
  }
})

/**
 * 清除本地存储中的登录凭证
 * user 是历史版本遗留的旧键名，一并清掉避免脏数据残留
 */
const clearStoredLogin = () => {
  localStorage.removeItem('token')
  localStorage.removeItem('userInfo')
  localStorage.removeItem('user')
}

/**
 * 校验本地 localStorage 中保存的 Token 是否真实有效
 * 调 /auth/user：Token 有效返回用户信息；无效（过期/签名不匹配/用户不存在）返回 null。
 * 后端重启后 JWT 随机密钥变化，旧 Token 会被判定为无效，
 * 从而避免前端"显示已登录但调用接口全部无权限"的状态错乱。
 */
export const validateStoredToken = async () => {
  const token = localStorage.getItem('token')
  if (!token) return null
  try {
    const response = await silentRequest.get('/auth/user', {
      headers: { Authorization: `Bearer ${token}` }
    })
    const body = response.data
    if (body && body.code === 200 && body.data) {
      return body.data
    }
    return null
  } catch (e) {
    return null
  }
}

/**
 * 请求拦截器：每次发请求前自动补全 Authorization header
 * - 从 localStorage.getItem('token') 取 JWT（登录接口写入）
 * - 有 token 就加 Bearer 前缀
 */
request.interceptors.request.use(
  config => {
    const token = localStorage.getItem('token')
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  error => {
    return Promise.reject(error)
  }
)

/**
 * 响应拦截器：统一处理 ApiResponse 包装体
 *
 * 后端 ApiResponse<T> = { code, message, data }
 * 本拦截器处理策略：
 *  Step 1: response.data → 兼容后端偶发返回 JSON 字符串格式，先 JSON.parse
 *  Step 2: code !== 200 → 弹 ElMessage 错误并 reject（业务层直接走 catch）
 *  Step 3: 正常返回 → 取出 data 字段给业务层（前端永远不用判断 res.code）
 *
 *  例：const user = await getProfile() → user 就是后端 ApiResponse.data 的 User 对象
 */
request.interceptors.response.use(
  response => {
    console.log('Response received:', response.status, response.data)
    let res = response.data

    // 兼容后端偶发返回 "string 类型 JSON" 场景
    if (typeof res === 'string') {
      try {
        res = JSON.parse(res)
      } catch (e) {
        return res
      }
    }

    // code ≠ 200 → 业务错误，弹提示 + reject 让业务层 catch 处理
    if (res && res.code !== undefined && res.code !== 200) {
      ElMessage.error(res.message || '请求失败')
      return Promise.reject(new Error(res.message || 'Error'))
    }

    // 正常：直接解包 data 字段（ApiResponse.data）
    return res && res.data !== undefined ? res.data : res
  },
  /**
   * 错误拦截器：按 HTTP 状态码统一处理
   *  - 401：登录过期 → 清 localStorage 跳 /login
   *  - 403：权限不足（CSRF/JWT 无效/没有权限）
   *  - 404/500/502/504：对应中文提示
   *  - 没 response：网络断开
   */
  error => {
    if (error.response) {
      const status = error.response.status
      const message = error.response.data?.message || '请求失败'

      switch (status) {
        case 401:
          ElMessage.error('登录已过期，请重新登录')
          clearStoredLogin()
          window.location.href = '/login'
          break
        case 403:
          // 本项目没有角色区分：带 Token 仍返回 403，只可能是 Token 无效/未登录，等同登录失效
          // 必须清掉本地登录态并回登录页，否则会卡在"显示已登录但接口全无权限"的错乱状态
          ElMessage.error('登录状态已失效，请重新登录')
          clearStoredLogin()
          window.location.href = '/login'
          break
        case 404:
          ElMessage.error('资源未找到')
          break
        case 500:
          ElMessage.error('服务器内部错误')
          break
        default:
          ElMessage.error(message)
      }
    } else if (error.request) {
      ElMessage.error('网络连接失败，请检查网络')
    } else {
      ElMessage.error('请求配置错误')
    }

    return Promise.reject(error)
  }
)

export default request