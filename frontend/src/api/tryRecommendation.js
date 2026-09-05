import api from './index'

export const tryRecommendationApi = {
  list: () => api.get('/try-recommendations').then(r => r.data),
  quota: () => api.get('/try-recommendations/quota').then(r => r.data),
  create: (periodStart, periodEnd) =>
    api.post('/try-recommendations', { periodStart, periodEnd }).then(r => r.data),
  delete: (id) => api.delete(`/try-recommendations/${id}`)
}
