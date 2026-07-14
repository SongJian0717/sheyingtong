import request from '../utils/request'

export const getRecommendations = (requirement) => {
  return request.post('/gear/recommend', requirement)
}

export const searchGear = (params) => {
  const queryParams = new URLSearchParams()
  if (params.keyword) queryParams.append('keyword', params.keyword)
  if (params.categoryId) queryParams.append('categoryId', params.categoryId)
  if (params.brand) queryParams.append('brand', params.brand)
  if (params.minPrice) queryParams.append('minPrice', params.minPrice)
  if (params.maxPrice) queryParams.append('maxPrice', params.maxPrice)
  if (params.page !== undefined) queryParams.append('page', params.page)
  if (params.size !== undefined) queryParams.append('size', params.size)
  return request.get(`/gear/search?${queryParams.toString()}`)
}

export const getGearDetail = (id) => {
  return request.get(`/gear/${id}`)
}

export const getCategories = () => {
  return request.get('/gear/categories')
}

export const getPopularGear = () => {
  return request.get('/gear/popular')
}