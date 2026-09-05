import api from './index'

export const statisticsApi = {
  getScoreTrend: (periodStart, periodEnd) =>
    api.get('/statistics', { params: { periodStart, periodEnd } }).then(r => r.data)
}
