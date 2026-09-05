import api from './index'

export const kptMemoApi = {
  list: () =>
    api.get('/kpt-notes').then(r => r.data),

  create: (type, content) =>
    api.post('/kpt-notes', { type, content }),

  update: (id, content) =>
    api.put(`/kpt-notes/${id}`, { content }),

  delete: (id) =>
    api.delete(`/kpt-notes/${id}`),

  reorder: (type, orderedIds) =>
    api.put('/kpt-notes/order', { type, orderedIds })
}
