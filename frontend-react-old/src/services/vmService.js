import api from './api'

export const vmService = {
  listVms: (userId) =>
    api.get(`/api/infrastructure/vps/user/${userId}`).then((r) => r.data),

  createVm: (data) =>
    api.post('/api/infrastructure/vps', data).then((r) => r.data),

  getVm: (id) =>
    api.get(`/api/infrastructure/vps/${id}`).then((r) => r.data),

  deleteVm: (id) =>
    api.delete(`/api/infrastructure/vps/${id}`).then((r) => r.data),

  restartVm: (id) =>
    api.post(`/api/infrastructure/vps/${id}/restart`).then((r) => r.data),

  stopVm: (id) =>
    api.post(`/api/infrastructure/vps/${id}/stop`).then((r) => r.data),
}