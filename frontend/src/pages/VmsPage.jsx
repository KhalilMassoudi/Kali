import { useEffect, useState } from 'react'
import { Plus, RefreshCw, Server } from 'lucide-react'
import { useAuthStore } from '../store/authStore'
import { useVmStore } from '../store/vmStore'
import VmCard from '../components/VmCard'
import CreateVmModal from '../components/CreateVmModal'
import LoadingSpinner from '../components/LoadingSpinner'

const FILTERS = [
  { key: 'all',     label: 'Toutes' },
  { key: 'running', label: 'En marche' },
  { key: 'stopped', label: 'Arrêtées' },
  { key: 'error',   label: 'En erreur' },
]

export default function VmsPage() {
  const user = useAuthStore((s) => s.user)
  const { vms, loading, error, fetchVms, clearError } = useVmStore()
  const [showModal, setShowModal] = useState(false)
  const [filter, setFilter] = useState('all')

  useEffect(() => {
    if (user?.id) fetchVms(user.id)
  }, [user?.id])

  const filtered = vms.filter((vm) => {
    if (filter === 'all')     return true
    if (filter === 'running') return ['ACTIVE', 'RUNNING'].includes(vm.status)
    if (filter === 'stopped') return vm.status === 'STOPPED'
    if (filter === 'error')   return vm.status === 'ERROR'
    return true
  })

  const counts = {
    all:     vms.length,
    running: vms.filter(v => ['ACTIVE', 'RUNNING'].includes(v.status)).length,
    stopped: vms.filter(v => v.status === 'STOPPED').length,
    error:   vms.filter(v => v.status === 'ERROR').length,
  }

  return (
    <div className="w-full px-5 py-5">
      {/* Header */}
      <div className="mb-5 flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
        <div>
          <h2 className="text-xl font-bold text-white">Machines Virtuelles</h2>
          <p className="mt-0.5 text-xs text-slate-500">
            {vms.length} VM{vms.length !== 1 ? 's' : ''} dans votre infrastructure
          </p>
        </div>
        <div className="flex gap-2 shrink-0">
          <button
            onClick={() => fetchVms(user?.id)}
            disabled={loading}
            className="flex items-center gap-1.5 rounded-lg px-3 py-2 text-xs font-semibold text-slate-400 hover:text-white disabled:opacity-50 transition-colors"
            style={{ border: '1px solid rgba(26,51,84,0.8)', backgroundColor: '#0E1825' }}
          >
            <RefreshCw className={`h-3.5 w-3.5 ${loading ? 'animate-spin' : ''}`} />
            Actualiser
          </button>
          <button
            onClick={() => setShowModal(true)}
            className="flex items-center gap-1.5 rounded-lg px-4 py-2 text-xs font-bold text-white hover:opacity-90 transition-all"
            style={{ background: 'linear-gradient(135deg, #0EA5E9, #38BDF8)', boxShadow: '0 4px 16px rgba(14,165,233,0.3)' }}
          >
            <Plus className="h-3.5 w-3.5" /> Créer une VM
          </button>
        </div>
      </div>

      {error && (
        <div
          className="mb-4 flex items-center gap-3 rounded-xl px-4 py-3"
          style={{ border: '1px solid rgba(239,68,68,0.3)', backgroundColor: 'rgba(239,68,68,0.06)' }}
        >
          <div className="h-1.5 w-1.5 rounded-full bg-red-500 shrink-0" />
          <p className="text-xs font-medium text-red-300 flex-1">{error}</p>
          <button onClick={clearError} className="text-red-400 hover:text-red-300 text-xs font-semibold">Fermer</button>
        </div>
      )}

      {/* Filters */}
      <div className="mb-5 flex gap-2 overflow-x-auto pb-1">
        {FILTERS.map(({ key, label }) => (
          <button
            key={key}
            onClick={() => setFilter(key)}
            className={`shrink-0 flex items-center gap-1.5 rounded-full px-3.5 py-1.5 text-xs font-semibold transition-all ${
              filter === key ? 'text-white' : 'text-slate-400 hover:text-white'
            }`}
            style={
              filter === key
                ? { background: 'linear-gradient(135deg, #0EA5E9, #0284C7)', boxShadow: '0 2px 10px rgba(14,165,233,0.3)' }
                : { border: '1px solid rgba(26,51,84,0.8)', backgroundColor: '#0E1825' }
            }
          >
            {label}
            <span
              className="rounded-full px-1.5 py-0.5 text-[10px] font-bold"
              style={{ backgroundColor: filter === key ? 'rgba(255,255,255,0.2)' : '#1A3354' }}
            >
              {counts[key]}
            </span>
          </button>
        ))}
      </div>

      {/* Content */}
      {loading && vms.length === 0 ? (
        <div className="flex justify-center py-20">
          <LoadingSpinner size="lg" text="Chargement des VMs..." />
        </div>
      ) : filtered.length === 0 ? (
        <div
          className="flex flex-col items-center gap-5 rounded-2xl py-16 text-center"
          style={{ border: '1px solid rgba(26,51,84,0.8)', backgroundColor: '#0E1825' }}
        >
          <div className="flex h-20 w-20 items-center justify-center rounded-2xl" style={{ backgroundColor: '#131F2E' }}>
            <Server className="h-10 w-10 text-slate-600" />
          </div>
          <div>
            <p className="text-base font-bold text-slate-300">
              {filter === 'all' ? 'Aucune VM pour le moment' : `Aucune VM ${FILTERS.find(f => f.key === filter)?.label.toLowerCase()}`}
            </p>
            <p className="mt-1 text-xs text-slate-500">
              {filter === 'all' ? 'Déployez votre première machine virtuelle en quelques clics.' : 'Essayez un autre filtre.'}
            </p>
          </div>
          {filter === 'all' && (
            <button
              onClick={() => setShowModal(true)}
              className="flex items-center gap-1.5 rounded-lg px-5 py-2.5 text-sm font-bold text-white hover:opacity-90 transition-all"
              style={{ background: 'linear-gradient(135deg, #0EA5E9, #38BDF8)', boxShadow: '0 4px 16px rgba(14,165,233,0.3)' }}
            >
              <Plus className="h-4 w-4" /> Créer une VM
            </button>
          )}
        </div>
      ) : (
        <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4">
          {filtered.map((vm) => (
            <VmCard key={vm.id} vm={vm} onDeleted={() => fetchVms(user?.id)} />
          ))}
        </div>
      )}

      {showModal && (
        <CreateVmModal
          onClose={() => setShowModal(false)}
          onCreated={() => fetchVms(user?.id)}
        />
      )}
    </div>
  )
}