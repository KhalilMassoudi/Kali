import { useState } from 'react'
import { X, Server, Cpu, HardDrive, MemoryStick } from 'lucide-react'
import { useVmStore } from '../store/vmStore'
import { useAuthStore } from '../store/authStore'
import { Field, Input, Select } from './ui/Input'
import Button from './ui/Button'

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

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/70 p-4 backdrop-blur-sm">
      <div className="w-full max-w-lg border border-brand-border bg-brand-surface shadow-2xl">
        {/* Header */}
        <div className="flex items-center justify-between border-b border-brand-border p-6">
          <div className="flex items-center gap-3">
            <div className="flex h-10 w-10 items-center justify-center bg-primary/10">
              <Server className="h-5 w-5 text-primary" />
            </div>
            <div>
              <h2 className="text-lg font-bold text-white">Créer une VM</h2>
              <p className="text-xs text-slate-500">Configurez votre machine virtuelle</p>
            </div>
          </div>
          <Button variant="ghost" size="icon" onClick={onClose} className="p-2">
            <X className="h-5 w-5" />
          </Button>
        </div>

        {/* Form */}
        <form onSubmit={handleSubmit} className="space-y-5 p-6">
          {error && (
            <div className="flex items-center gap-3 border border-crit/30 bg-crit/[0.08] px-4 py-3.5">
              <div className="h-2 w-2 shrink-0 rounded-full bg-crit" />
              <p className="text-sm font-medium text-crit">{error}</p>
            </div>
          )}

          <Field label="Nom de la VM" hint="Lettres minuscules, chiffres et tirets uniquement">
            <Input
              placeholder="mon-serveur-web"
              value={form.name}
              onChange={(e) => { set('name', e.target.value.toLowerCase()); setError('') }}
            />
          </Field>

          <Field label="Système d'exploitation">
            <Select value={form.os} onChange={(e) => set('os', e.target.value)}>
              {OS_OPTIONS.map((o) => (
                <option key={o.value} value={o.value}>{o.label} — {o.desc}</option>
              ))}
            </Select>
          </Field>

          <div className="grid grid-cols-2 gap-4">
            <Field label="RAM" icon={MemoryStick}>
              <Select value={form.ram} onChange={(e) => set('ram', e.target.value)}>
                {RAM_OPTIONS.map((o) => <option key={o.value} value={o.value}>{o.label}</option>)}
              </Select>
            </Field>
            <Field label="CPU (vCPU)" icon={Cpu}>
              <Select value={form.cpu} onChange={(e) => set('cpu', e.target.value)}>
                {[1, 2, 4, 8].map((v) => <option key={v} value={v}>{v} vCPU{v > 1 ? 's' : ''}</option>)}
              </Select>
            </Field>
          </div>

          <Field label="Stockage" icon={HardDrive}>
            <Select value={form.storage} onChange={(e) => set('storage', e.target.value)}>
              {[10, 20, 50, 100, 200].map((v) => <option key={v} value={v}>{v} GB</option>)}
            </Select>
          </Field>

          {/* Footer */}
          <div className="flex gap-3 pt-2">
            <Button type="button" variant="secondary" size="lg" onClick={onClose} className="flex-1">
              Annuler
            </Button>
            <Button type="submit" variant="primary" size="lg" loading={loading} className="flex-1">
              {loading ? 'Création...' : 'Créer la VM'}
            </Button>
          </div>
        </form>
      </div>
    </div>
  )
}