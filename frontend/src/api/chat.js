/**
 * 聊天 API 封装（REST + SSE 两类接口）
 * ----------------------------------------------------------------
 * 【REST 接口 - 走 axios + 统一拦截器】
 *  getSessions() / createSession() / getSession() / deleteSession()
 *  getMessages() / sendMessage() / createNewSession()
 *
 * 【SSE 流式接口 - 走原生 XMLHttpRequest，不能走 axios】
 *  ⚠️  为什么不用 axios/fetch？
 *    - axios responseType: 'stream' 在浏览器无效（只支持 Node.js）
 *    - fetch ReadableStream + getReader() 可以，但兼容旧浏览器和 CancelToken 较差
 *    - XMLHttpRequest onprogress 在 99% 浏览器稳定支持 text/event-stream 增量拼接
 *
 *  sendMessageStream(sessionId, content, onChunk, onError)
 *     → 纯文字流式，POST /api/chat/session/{id}/message/stream
 *  sendMessageWithImage(sessionId, content, file, onChunk)
 *     → 图片多模态流式，POST /api/chat/session/{id}/message/image（multipart/form-data）
 *
 *  SSE 解析流程：
 *   1) lastProcessedIndex 记录上次已处理的 responseText 末尾
 *   2) onprogress 触发时 substring() 取出新增内容，按 \n 切成 SSE 行
 *   3) 每行用正则去 "data: " 前缀，拼到 fullResponse
 *   4) onChunk 回调给 ChatPage.vue，ChatPage 把 token 实时 append 到消息气泡
 *   5) onload 时 status 401/403 → 拒绝 Promise（对应 JWT 过期 / 无权限）
 *   6) 每个流式请求都要手动 setRequestHeader Authorization（XHR 不会走 axios 拦截器）
 */
import request from '../utils/request'

export const getSessions = () => {
  return request({
    url: '/chat/sessions',
    method: 'get'
  })
}

export const createSession = (title) => {
  return request({
    url: '/chat/session',
    method: 'post',
    data: title ? { title } : {}
  })
}

export const getSession = (sessionId) => {
  return request({
    url: `/chat/session/${sessionId}`,
    method: 'get'
  })
}

export const deleteSession = (sessionId) => {
  return request({
    url: `/chat/session/${sessionId}`,
    method: 'delete'
  })
}

export const getMessages = (sessionId) => {
  return request({
    url: `/chat/session/${sessionId}/messages`,
    method: 'get'
  })
}

export const sendMessage = (sessionId, content) => {
  return request({
    url: `/chat/session/${sessionId}/message`,
    method: 'post',
    data: { content }
  })
}

/**
 * 【SSE 流式聊天：纯文字】打字机效果接口
 * ----------------------------------------------------------------
 * XMLHttpRequest 手动实现：
 *  - 必须手动加 Content-Type / Accept / Authorization 头
 *    （XHR 不走 axios 拦截器，之前缺这一行导致 Spring Security 403）
 *  - onprogress 每个 SSE chunk 到达时触发 → lastProcessedIndex 增量切片
 *  - onChunk(data) 回调：把新 token 拼到 ChatPage 气泡里（Vue 响应式驱动 UI 实时渲染）
 *  - onload 200：resolve(fullResponse) → 保存完整回答到历史
 *  - onload 401/403：onError 弹中文提示 + reject
 *
 * @param {number} sessionId  会话 ID
 * @param {string} content    用户消息文本
 * @param {Function(data:string)} onChunk  每来一个 token 触发
 * @param {Function(msg:string)}  onError  401/403/网络 等错误触发
 * @returns {Promise<string>} 完整 AI 回答内容
 */
export const sendMessageStream = (sessionId, content, onChunk, onError) => {
  return new Promise((resolve, reject) => {
    const xhr = new XMLHttpRequest()
    xhr.open('POST', `/api/chat/session/${sessionId}/message/stream`, true)
    xhr.setRequestHeader('Content-Type', 'application/json')
    xhr.setRequestHeader('Accept', 'text/event-stream')
    const token = localStorage.getItem('token')
    if (token) {
      xhr.setRequestHeader('Authorization', 'Bearer ' + token)
    }
    
    let fullResponse = ''
    let lastProcessedIndex = 0
    
    xhr.onprogress = function() {
      const response = xhr.responseText
      const newContent = response.substring(lastProcessedIndex)
      lastProcessedIndex = response.length
      
      const chunks = newContent.split('\n')
      for (const chunk of chunks) {
        if (chunk.trim()) {
          try {
            const data = chunk.replace(/^data:\s*/, '')
            if (data && data !== '[DONE]') {
              fullResponse += data
              onChunk && onChunk(data)
            }
          } catch (e) {
            console.error('解析流式数据失败:', e)
          }
        }
      }
    }
    
    xhr.onload = function() {
      if (xhr.status === 401 || xhr.status === 403) {
        onError && onError(xhr.status === 401 ? '登录已过期' : '权限不足')
        reject(new Error('HTTP ' + xhr.status))
        return
      }
      resolve(fullResponse)
    }
    
    xhr.onerror = function() {
      onError && onError('网络错误')
      reject(new Error('网络错误'))
    }
    
    xhr.send(JSON.stringify({ content }))
  })
}

export const createNewSession = () => {
  return request({
    url: '/chat/new-session',
    method: 'post'
  })
}

/**
 * 【SSE 流式聊天：图片 + 文字】多模态分析接口
 * ----------------------------------------------------------------
 * 区别于 sendMessageStream：
 *  - FormData 表单上传，content 文本字段 + image 文件字段
 *  - 不要手动设置 Content-Type，浏览器自动 multipart/form-data; boundary=XXX
 *  - 仍然手动加 Authorization: Bearer <JWT>（XHR 不走 axios 拦截器）
 *  - SSE 解析逻辑和 sendMessageStream 完全相同：lastProcessedIndex 增量 + data: 前缀剥离
 *
 * @param {number}   sessionId 会话 ID
 * @param {string}   content   用户提问文字
 * @param {File}     file      用户上传的图片 File 对象
 * @param {Function(data:string)} onChunk 每来一个 token 触发
 * @returns {Promise<string>} 完整 AI 回答内容
 */
export const sendMessageWithImage = (sessionId, content, file, onChunk) => {
  return new Promise((resolve, reject) => {
    const xhr = new XMLHttpRequest()
    xhr.open('POST', `/api/chat/session/${sessionId}/message/image`, true)
    
    xhr.setRequestHeader('Accept', 'text/event-stream')
    const token = localStorage.getItem('token')
    if (token) {
      xhr.setRequestHeader('Authorization', 'Bearer ' + token)
    }
    
    let fullResponse = ''
    let lastProcessedIndex = 0
    
    xhr.onprogress = function() {
      const response = xhr.responseText
      const newContent = response.substring(lastProcessedIndex)
      lastProcessedIndex = response.length
      
      const chunks = newContent.split('\n')
      for (const chunk of chunks) {
        if (chunk.trim()) {
          try {
            const data = chunk.replace(/^data:\s*/, '')
            if (data && data !== '[DONE]') {
              fullResponse += data
              onChunk && onChunk(data)
            }
          } catch (e) {
            console.error('解析流式数据失败:', e)
          }
        }
      }
    }
    
    xhr.onload = function() {
      if (xhr.status === 401 || xhr.status === 403) {
        reject(new Error('HTTP ' + xhr.status + (xhr.status === 401 ? ' 登录已过期' : ' 权限不足')))
        return
      }
      resolve(fullResponse)
    }
    
    xhr.onerror = function() {
      reject(new Error('网络错误'))
    }
    
    const formData = new FormData()
    formData.append('content', content)
    formData.append('image', file)
    
    xhr.send(formData)
  })
}