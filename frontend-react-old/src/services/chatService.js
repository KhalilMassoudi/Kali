import api from './api'

export const chatService = {
  sendMessage: (userId, content) =>
    api.post('/api/chat/message', { userId, content }).then((r) => r.data),

  getHistory: (userId) =>
    api.get(`/api/chat/history?userId=${userId}`).then((r) => r.data),
}