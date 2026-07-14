import request from '../utils/request'

export const login = (username, password) => {
  return request.post('/auth/login', {
    username,
    password
  })
}

export const register = (username, password, email) => {
  return request.post('/auth/register', {
    username,
    password,
    email
  })
}

export const getUserInfo = () => {
  return request.get('/auth/user')
}

export const logout = () => {
  return request.post('/auth/logout')
}

export const updateUserInfo = (userInfo) => {
  return request.put('/auth/user', userInfo)
}

export const changePassword = (passwordData) => {
  return request.post('/auth/user/change-password', passwordData)
}

export const getUserStatistics = () => {
  return request.get('/auth/user/statistics')
}
