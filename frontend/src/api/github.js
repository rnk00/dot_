import axios from 'axios'

const api = axios.create({ baseURL: '/api/github' })

api.interceptors.request.use(config => {
  const token = localStorage.getItem('token')
  if (token) config.headers.Authorization = `Bearer ${token}`
  return config
})

export const githubApi = {
  updateSettings: (token, repo) =>
    api.put('/settings', { token, repo }).then(r => r.data),

  push: (retrospectId) =>
    api.post(`/push/${retrospectId}`).then(r => r.data),
}
