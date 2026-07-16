import api from './api'

export const vmService = {
  listVms: (userId) =>
    api.get(`/api/vps?userId=${userId}`).then((r) => r.data),

  createVm: (data) =>
    api.post('/api/vps', data).then((r) => r.data),

  getVm: (id) =>
    api.get(`/api/vps/${id}`).then((r) => r.data),

  deleteVm: (id) =>
    api.delete(`/api/vps/${id}`).then((r) => r.data),

  restartVm: (id) =>
    api.post(`/api/vps/${id}/restart`).then((r) => r.data),

  stopVm: (id) =>
    api.post(`/api/vps/${id}/stop`).then((r) => r.data),
}