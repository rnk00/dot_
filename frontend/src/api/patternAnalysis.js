import api from './index'

export const patternAnalysisApi = {
  list: () => api.get('/pattern-analyses').then(r => r.data),
  quota: () => api.get('/pattern-analyses/quota').then(r => r.data),
  create: (periodStart, periodEnd) =>
    api.post('/pattern-analyses', { periodStart, periodEnd }).then(r => r.data),
  delete: (id) => api.delete(`/pattern-analyses/${id}`)
}
