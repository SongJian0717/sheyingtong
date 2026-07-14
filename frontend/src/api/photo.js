import request from '../utils/request'

export const scorePhoto = (file, description = '') => {
  const formData = new FormData()
  formData.append('file', file)
  if (description) {
    formData.append('description', description)
  }
  return request.post('/photo/score', formData, {
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  })
}

export const getScoreHistory = (page = 0, size = 10) => {
  const params = new URLSearchParams()
  params.append('page', page)
  params.append('size', size)
  return request.get(`/photo/history?${params.toString()}`)
}

export const getPhotoDetail = (id) => {
  return request.get(`/photo/detail/${id}`)
}

export const deleteAnalysis = (id) => {
  return request.delete(`/photo/${id}`)
}