import api from './index'

export const retrospectApi = {
  getCalendar: (year, month) =>
    api.get('/retrospects/calendar', { params: { year, month } }).then(r => r.data),

  getByDate: (date) =>
    api.get(`/retrospects/date/${date}`).then(r => r.data),

  upsertScore: (date, score) =>
    api.put(`/retrospects/date/${date}/score`, { score }).then(r => r.data),

  delete: (id) =>
    api.delete(`/retrospects/${id}`),

  addItem: (date, type, content) =>
    api.post(`/retrospects/date/${date}/kpt-items`, { type, content }).then(r => r.data),

  updateItem: (date, itemId, content) =>
    api.put(`/retrospects/date/${date}/kpt-items/${itemId}`, { content }).then(r => r.data),

  deleteItem: (date, itemId) =>
    api.delete(`/retrospects/date/${date}/kpt-items/${itemId}`).then(r => r.data),

  reorderItems: (date, type, orderedIds) =>
    api.put(`/retrospects/date/${date}/kpt-items/order`, { type, orderedIds }).then(r => r.data),
}
