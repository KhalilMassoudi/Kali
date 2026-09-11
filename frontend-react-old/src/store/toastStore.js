import { create } from 'zustand'

let nextId = 1

export const useToastStore = create((set) => ({
  toasts: [],

  addToast: (message, type = 'error', duration = 5000) => {
    const id = nextId++
    set((s) => ({ toasts: [...s.toasts, { id, message, type, duration }] }))
    setTimeout(() => {
      set((s) => ({ toasts: s.toasts.filter((t) => t.id !== id) }))
    }, duration)
  },

  removeToast: (id) => set((s) => ({ toasts: s.toasts.filter((t) => t.id !== id) })),
}))

export const toast = {
  error: (msg) => useToastStore.getState().addToast(msg, 'error'),
  success: (msg) => useToastStore.getState().addToast(msg, 'success'),
  info: (msg) => useToastStore.getState().addToast(msg, 'info'),
}