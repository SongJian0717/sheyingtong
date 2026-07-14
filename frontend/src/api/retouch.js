import request from '../utils/request'

export const getRetouchSuggestions = (file, description) => {
  const formData = new FormData()
  formData.append('file', file)
  if (description) {
    formData.append('description', description)
  }
  return request.post('/retouch/suggestions', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

export const getRetouchHistory = (page, size) => {
  return request.get(`/retouch/history?page=${page}&size=${size}`)
}

export const deleteRetouchRecord = (id) => {
  return request.delete(`/retouch/${id}`)
}

export const getRetouchDetail = (id) => {
  return request.get(`/retouch/${id}`)
}