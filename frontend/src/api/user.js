import request from '../utils/request'

/** 用户公开主页（公开，按 id） */
export const getUserProfile = (userId) => {
  return request.get(`/users/${userId}`)
}

/** 用户公开主页（公开，按 username，前端 /user/:username 路由用） */
export const getUserProfileByUsername = (username) => {
  return request.get(`/users/username/${encodeURIComponent(username)}`)
}

/** 用户作品墙（公开，id 游标分页） */
export const getUserWorks = (userId, cursor = null, size = 20) => {
  const params = new URLSearchParams()
  if (cursor) params.append('cursor', cursor)
  params.append('size', size)
  return request.get(`/users/${userId}/works?${params.toString()}`)
}

/** 粉丝列表（公开，最多 50） */
export const getUserFollowers = (userId, size = 50) => {
  return request.get(`/users/${userId}/followers?size=${size}`)
}

/** 关注列表（公开，最多 50） */
export const getUserFollowing = (userId, size = 50) => {
  return request.get(`/users/${userId}/following?size=${size}`)
}

/** 关注用户（需登录） */
export const followUser = (userId) => {
  return request.post(`/users/${userId}/follow`)
}

/** 取消关注（需登录） */
export const unfollowUser = (userId) => {
  return request.delete(`/users/${userId}/follow`)
}
