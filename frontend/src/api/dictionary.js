import request from '../utils/request'

export const searchTerms = (query, topK = 5) => {
  return request.post('/dictionary/search', {
    query,
    topK
  })
}

export const getTermDetail = (term) => {
  return request.get(`/dictionary/term/${encodeURIComponent(term)}`)
}

export const getTerms = (page = 0, size = 20, category = '') => {
  const params = new URLSearchParams()
  params.append('page', page)
  params.append('size', size)
  if (category) {
    params.append('category', category)
  }
  return request.get(`/dictionary/terms?${params.toString()}`)
}

export const getCategories = () => {
  return request.get('/dictionary/categories')
}