import { useState } from 'react'
import { X, Loader2, Server, Cpu, HardDrive, MemoryStick } from 'lucide-react'
import { useVmStore } from '../store/vmStore'
import { useAuthStore } from '../store/authStore'

const OS_OPTIONS = [
  { value: 'ubuntu-22.04',   label: 'Ubuntu 22.04 LTS',   desc: 'Recommandé' },
  { value: 'debian-11',      label: 'Debian 11 Bullseye',  desc: 'Stable' },
  { value: 'centos-stream-9',label: 'CentOS Stream 9',     desc: 'Enterprise' },
  { value: 'cirros',         label: 'Cirros',              desc: 'Test léger' },
]

const RAM_OPTIONS = [
  { value: 512,  label: '512 MB' },
  { value: 1024, label: '1 GB' },
  { value: 2048, label: '2 GB' },
  { value: 4096, label: '4 GB' },
  { value: 8192, label: '8 GB' },
]

export default function CreateVmModal({ onClose, onCreated }) {
  const user = useAuthStore((s) => s.user)
  const { createVm, loading } = useVmStore()

  const [form, setForm] = useState({ name: '', os: 'ubuntu-22.04', ram: 2048, cpu: 2, storage: 20 })
  const [error, setError] = useState('')

  const set = (key, val) => setForm((f) => ({ ...f, [key]: val }))

  const validate = () => {
    if (!form.name.trim()) return 'Le nom est obligatoire'
    if (form.name.length < 3) return 'Minimum 3 caractères'
    if (!/^[a-z0-9-]+$/.test(form.name)) return 'Lettres minuscules, chiffres et tirets uniquement'
    return ''
  }

  const handleSubmit = async (e) => {
    e.preventDefault()
    const err = validate()
    if (err) { setError(err); return }
    setError('')
    try {
      await createVm({
        userId: user?.id || 1,
        name: form.name.trim(),
        os: form.os,
        ram: parseInt(form.ram),
        cpu: parseInt(form.cpu),
        storage: parseInt(form.storage),
      })
      onCreated?.()
      onClose()
    } catch (err) {
      setError(err.response?.data?.message || 'Erreur lors de la création')
    }
  }

  const inputClass = 'w-full rounded-xl px-4 py-3 text-sm text-white placeholder-gray-500 focus:outline-none transition-all focus:ring-2 focus:ring-primary/30'
  const labelClass = 'block text-sm font-semibold text-gray-300 mb-2'

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/70 backdrop-blur-sm p-4">
      <div
        className="w-full max-w-lg rounded-2xl shadow-2xl"
        style={{ border: '1px solid #1E3A5F', backgroundColor: '#111C2D' }}
      >
        {/* Header */}
        <div className="flex items-center justify-between p-6" style={{ borderBottom: '1px solid #1E3A5F' }}>
          <div className="flex items-center gap-3">
            <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-primary/10">
              <Server className="h-5 w-5 text-primary" />
            </div>
            <div>
              <h2 className="text-lg font-bold text-white">Créer une VM</h2>
              <p className="text-xs text-gray-500">Configurez votre machine virtuelle</p>
            </div>
          </div>
          <button onClick={onClose} className="rounded-xl p-2 text-gray-500 hover:bg-white/5 hover:text-white transition-colors">
            <X className="h-5 w-5" />
          </button>
        </div>

        {/* Form */}
        <form onSubmit={handleSubmit} className="p-6 space-y-5">
          {error && (
            <div className="flex items-center gap-3 rounded-xl px-4 py-3.5" style={{ border: '1px solid rgba(239,68,68,0.3)', backgroundColor: 'rgba(239,68,68,0.08)' }}>
              <div className="h-2 w-2 rounded-full bg-red-500 shrink-0" />
              <p className="text-sm font-medium text-red-300">{error}</p>
            </div>
          )}

          <div>
            <label className={labelClass}>Nom de la VM</label>
            <input
              className={inputClass}
              style={{ border: '1px solid #1E3A5F', backgroundColor: '#162030' }}
              placeholder="mon-serveur-web"
              value={form.name}
              onChange={(e) => { set('name', e.target.value.toLowerCase()); setError('') }}
            />
            <p className="mt-1.5 text-xs text-gray-600">Lettres minuscules, chiffres et tirets uniquement</p>
          </div>

          <div>
            <label className={labelClass}>Système d'exploitation</label>
            <select
              className={inputClass}
              style={{ border: '1px solid #1E3A5F', backgroundColor: '#162030' }}
              value={form.os}
              onChange={(e) => set('os', e.target.value)}
            >
              {OS_OPTIONS.map((o) => (
                <option key={o.value} value={o.value}>{o.label} — {o.desc}</option>
              ))}
            </select>
          </div>

          <div className="grid grid-cols-2 gap-4">
            <div>
              <label className={`${labelClass} flex items-center gap-1.5`}>
                <MemoryStick className="h-4 w-4 text-gray-500" /> RAM
              </label>
              <select
                className={inputClass}
                style={{ border: '1px solid #1E3A5F', backgroundColor: '#162030' }}
                value={form.ram}
                onChange={(e) => set('ram', e.target.value)}
              >
                {RAM_OPTIONS.map((o) => <option key={o.value} value={o.value}>{o.label}</option>)}
              </select>
            </div>
            <div>
              <label className={`${labelClass} flex items-center gap-1.5`}>
                <Cpu className="h-4 w-4 text-gray-500" /> CPU (vCPU)
              </label>
              <select
                className={inputClass}
                style={{ border: '1px solid #1E3A5F', backgroundColor: '#162030' }}
                value={form.cpu}
                onChange={(e) => set('cpu', e.target.value)}
              >
                {[1, 2, 4, 8].map((v) => <option key={v} value={v}>{v} vCPU{v > 1 ? 's' : ''}</option>)}
              </select>
            </div>
          </div>

          <div>
            <label className={`${labelClass} flex items-center gap-1.5`}>
              <HardDrive className="h-4 w-4 text-gray-500" /> Stockage
            </label>
            <select
              className={inputClass}
              style={{ border: '1px solid #1E3A5F', backgroundColor: '#162030' }}
              value={form.storage}
              onChange={(e) => set('storage', e.target.value)}
            >
              {[10, 20, 50, 100, 200].map((v) => <option key={v} value={v}>{v} GB</option>)}
            </select>
          </div>

          {/* Footer */}
          <div className="flex gap-3 pt-2">
            <button
              type="button"
              onClick={onClose}
              className="flex-1 rounded-xl py-3 text-sm font-semibold text-gray-400 hover:text-white transition-colors"
              style={{ border: '1px solid #1E3A5F' }}
            >
              Annuler
            </button>
            <button
              type="submit"
              disabled={loading}
              className="flex flex-1 items-center justify-center gap-2 rounded-xl py-3 text-sm font-bold text-white transition-all disabled:opacity-60 hover:opacity-90"
              style={{ background: 'linear-gradient(135deg, #00A3FF, #00D9FF)', boxShadow: '0 4px 20px rgba(0,163,255,0.3)' }}
            >
              {loading ? <><Loader2 className="h-4 w-4 animate-spin" /> Création...</> : 'Créer la VM'}
            </button>
          </div>
        </form>
      </div>
    </div>
  )
}