import request from '../utils/request'

/**
 * 作品图片地址：AI 打分原始图走后端公开图片接口（/api/photo/image/{photoAnalysisId}）
 * 社区模块不复制文件，只引用 photo_analysis_id，因此这里统一拼 URL。
 */
export const workImageUrl = (photoAnalysisId) => `/api/photo/image/${photoAnalysisId}`

/**
 * 作品广场（公开）
 * sort: latest=最新发布（id 游标倒序）| hot=热门榜（点赞数 Top-N）
 * cursor: 上一页返回的 nextCursor；size 默认 20
 */
export const getWorks = (sort = 'latest', cursor = null, size = 20) => {
  const params = new URLSearchParams()
  params.append('sort', sort)
  if (cursor) params.append('cursor', cursor)
  params.append('size', size)
  return request.get(`/works?${params.toString()}`)
}

/** 我的作品（需登录） */
export const getMyWorks = () => {
  return request.get('/works/mine')
}

/** 作品详情（公开；含当前登录者是否已点赞） */
export const getWorkDetail = (id) => {
  return request.get(`/works/${id}`)
}

/** 发布作品（需登录）：photoAnalysisId 来自我的打分历史 */
export const publishWork = (data) => {
  return request.post('/works', data)
}

/** 删除本人作品（需登录） */
export const deleteWork = (id) => {
  return request.delete(`/works/${id}`)
}

/** 点赞（需登录） */
export const likeWork = (id) => {
  return request.post(`/works/${id}/like`)
}

/** 取消点赞（需登录） */
export const unlikeWork = (id) => {
  return request.delete(`/works/${id}/like`)
}

/** 评论列表（公开，id 游标分页） */
export const getComments = (workId, cursor = null, size = 20) => {
  const params = new URLSearchParams()
  if (cursor) params.append('cursor', cursor)
  params.append('size', size)
  return request.get(`/works/${workId}/comments?${params.toString()}`)
}

/** 发表评论（需登录）：data = { content, parentId? } */
export const addComment = (workId, data) => {
  return request.post(`/works/${workId}/comments`, data)
}

/** 删除评论（需登录） */
export const deleteComment = (commentId) => {
  return request.delete(`/works/comments/${commentId}`)
}
