import { create } from 'zustand'
import { vmService } from '../services/vmService'

export const useVmStore = create((set, get) => ({
  vms: [],
  loading: false,
  error: null,

  fetchVms: async (userId) => {
    set({ loading: true, error: null })
    try {
      const vms = await vmService.listVms(userId)
      set({ vms: Array.isArray(vms) ? vms : [], loading: false })
    } catch (err) {
      set({ error: err.response?.data?.message || 'Erreur lors du chargement des VMs', loading: false })
    }
  },

  createVm: async (data) => {
    set({ loading: true, error: null })
    try {
      const vm = await vmService.createVm(data)
      set((s) => ({ vms: [...s.vms, vm], loading: false }))
      return vm
    } catch (err) {
      set({ error: err.response?.data?.message || 'Erreur lors de la création de la VM', loading: false })
      throw err
    }
  },

  deleteVm: async (id) => {
    set({ loading: true, error: null })
    try {
      await vmService.deleteVm(id)
      set((s) => ({ vms: s.vms.filter((v) => v.id !== id), loading: false }))
    } catch (err) {
      set({ error: err.response?.data?.message || 'Erreur lors de la suppression', loading: false })
      throw err
    }
  },

  restartVm: async (id) => {
    try {
      await vmService.restartVm(id)
      set((s) => ({
        vms: s.vms.map((v) => v.id === id ? { ...v, status: 'RUNNING' } : v),
      }))
    } catch (err) {
      set({ error: err.response?.data?.message || 'Erreur lors du redémarrage' })
      throw err
    }
  },

  stopVm: async (id) => {
    try {
      await vmService.stopVm(id)
      set((s) => ({
        vms: s.vms.map((v) => v.id === id ? { ...v, status: 'STOPPED' } : v),
      }))
    } catch (err) {
      set({ error: err.response?.data?.message || 'Erreur lors de l\'arrêt' })
      throw err
    }
  },

  clearError: () => set({ error: null }),
}))