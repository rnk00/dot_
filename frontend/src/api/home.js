import api from './index'

export const homeApi = {
  getSummary: () => api.get('/home-summary').then(r => r.data)
}
