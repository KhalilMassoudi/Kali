import { create } from 'zustand'
import { authService } from '../services/authService'

export const useAuthStore = create((set, get) => ({
  user: null,
  token: localStorage.getItem('token') || null,
  isLoggedIn: !!localStorage.getItem('token'),
  loading: false,
  error: null,

  login: async (email, password) => {
    set({ loading: true, error: null })
    try {
      const data = await authService.login(email, password)
      localStorage.setItem('token', data.token)
      set({ token: data.token, isLoggedIn: true, loading: false })
      await get().getMe()
    } catch (err) {
      const msg = err.response?.data?.message || 'Identifiants incorrects'
      set({ error: msg, loading: false })
      throw err
    }
  },

  register: async (email, password, firstName, lastName) => {
    set({ loading: true, error: null })
    try {
      const data = await authService.register(email, password, firstName, lastName)
      localStorage.setItem('token', data.token)
      set({ token: data.token, isLoggedIn: true, loading: false })
      await get().getMe()
    } catch (err) {
      const msg = err.response?.data?.message || 'Erreur lors de l\'inscription'
      set({ error: msg, loading: false })
      throw err
    }
  },

  getMe: async () => {
    try {
      const user = await authService.getMe()
      set({ user, isLoggedIn: true })
    } catch {
      get().logout()
    }
  },

  logout: () => {
    authService.logout()
    set({ user: null, token: null, isLoggedIn: false })
  },

  clearError: () => set({ error: null }),
}))