import api from './api'

export const authService = {
  login: (email, password) =>
    api.post('/api/auth/login', { email, password }).then((r) => r.data),

  register: (email, password, firstName, lastName) =>
    api.post('/api/auth/register', { email, password, firstName, lastName }).then((r) => r.data),

  getMe: () =>
    api.get('/api/auth/me').then((r) => r.data),

  logout: () => {
    localStorage.removeItem('token')
  },
}