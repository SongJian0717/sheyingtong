import request from '../utils/request'

/**
 * AI 异步任务 API（照片打分 / 修图建议共用）
 * ----------------------------------------------------------------
 * 提交接口（POST /photo/score、POST /retouch/suggestions）现在立即返回 {taskId}，
 * 真正的 AI 分析在后台消费者线程执行。页面拿到 taskId 后轮询这里两个接口：
 *  - getAiTask  ：查状态，terminal=true 后从 result 取最终结果（结构与改造前同步返回体一致）
 *  - retryAiTask：FAILED / DEGRADED 状态手动重试（复用已上传的原图，不需要重新上传）
 */
export const getAiTask = (taskId) => {
  return request.get(`/ai/tasks/${taskId}`)
}

export const retryAiTask = (taskId) => {
  return request.post(`/ai/tasks/${taskId}/retry`)
}