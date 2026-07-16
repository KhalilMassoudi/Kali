import { create } from 'zustand'
import { chatService } from '../services/chatService'

export const useChatStore = create((set) => ({
  messages: [],
  loading: false,
  error: null,

  sendMessage: async (userId, content) => {
    const userMsg = { id: Date.now(), role: 'user', content, timestamp: new Date().toISOString() }
    set((s) => ({ messages: [...s.messages, userMsg], loading: true, error: null }))
    try {
      const data = await chatService.sendMessage(userId, content)
      const botMsg = {
        id: Date.now() + 1,
        role: 'assistant',
        content: data.response || data.content || data.message || JSON.stringify(data),
        timestamp: new Date().toISOString(),
      }
      set((s) => ({ messages: [...s.messages, botMsg], loading: false }))
    } catch (err) {
      const errMsg = { id: Date.now() + 1, role: 'error', content: err.response?.data?.message || 'Erreur de communication', timestamp: new Date().toISOString() }
      set((s) => ({ messages: [...s.messages, errMsg], loading: false, error: errMsg.content }))
    }
  },

  getConversations: async (userId) => {
    set({ loading: true, error: null })
    try {
      const data = await chatService.getHistory(userId)
      const msgs = Array.isArray(data) ? data : []
      set({ messages: msgs, loading: false })
    } catch {
      set({ loading: false })
    }
  },

  clearMessages: () => set({ messages: [], error: null }),
  clearError: () => set({ error: null }),
}))