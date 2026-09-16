import request from '../utils/request'

/** 关注动态 Feed（需登录）：我关注的人发布的最新公开作品 */
export const getFeed = (cursor = null, size = 20) => {
  const params = new URLSearchParams()
  if (cursor) params.append('cursor', cursor)
  params.append('size', size)
  return request.get(`/feed?${params.toString()}`)
}
