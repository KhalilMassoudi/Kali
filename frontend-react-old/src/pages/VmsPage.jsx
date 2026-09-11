import { useEffect, useState } from 'react'
import { Plus, RefreshCw, Server } from 'lucide-react'
import { useAuthStore } from '../store/authStore'
import { useVmStore } from '../store/vmStore'
import FleetTable from '../components/FleetTable'
import CreateVmModal from '../components/CreateVmModal'
import LoadingSpinner from '../components/LoadingSpinner'
import { Card } from '../components/ui/Card'
import Button from '../components/ui/Button'

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
    <div className="w-full px-6 py-6">
      {/* Header */}
      <div className="mb-6 flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
        <div>
          <h2 className="text-xl font-bold text-white">Machines virtuelles</h2>
          <p className="mt-0.5 text-xs text-text-dim">
            {vms.length} VM{vms.length !== 1 ? 's' : ''} dans votre infrastructure
          </p>
        </div>
        <div className="flex gap-2 shrink-0">
          <Button variant="secondary" onClick={() => fetchVms(user?.id)} disabled={loading}>
            <RefreshCw className={`h-3.5 w-3.5 ${loading ? 'animate-spin' : ''}`} />
            Actualiser
          </Button>
          <Button variant="primary" onClick={() => setShowModal(true)}>
            <Plus className="h-3.5 w-3.5" /> Créer une VM
          </Button>
        </div>
      </div>

      {error && (
        <div className="mb-4 flex items-center gap-3 px-4 py-3 border border-crit/30 bg-crit/[0.06]">
          <div className="h-1.5 w-1.5 bg-crit shrink-0" />
          <p className="text-xs font-medium text-crit flex-1">{error}</p>
          <button onClick={clearError} className="text-crit hover:opacity-80 text-xs font-semibold transition-opacity">Fermer</button>
        </div>
      )}

      {/* Filters */}
      <div className="mb-5 flex gap-2 overflow-x-auto pb-1">
        {FILTERS.map(({ key, label }) => (
          <button
            key={key}
            onClick={() => setFilter(key)}
            className={`shrink-0 flex items-center gap-2 px-3.5 py-1.5 text-xs font-bold transition-all ${
              filter === key
                ? 'text-white bg-[linear-gradient(135deg,#2E9EE0,#5CC8F7)]'
                : 'text-slate-400 hover:text-white border border-brand-border bg-brand-surface'
            }`}
          >
            {label}
            <span
              className={`font-mono px-1.5 py-0.5 text-[10px] font-bold ${
                filter === key ? 'bg-brand-bg/25 text-brand-bg' : 'bg-brand-surface2 text-text-faint'
              }`}
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
        <Card className="flex flex-col items-center gap-5 py-16 text-center">
          <div className="flex h-20 w-20 items-center justify-center bg-brand-surface2">
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
            <Button variant="primary" size="lg" onClick={() => setShowModal(true)}>
              <Plus className="h-4 w-4" /> Créer une VM
            </Button>
          )}
        </Card>
      ) : (
        <Card>
          <FleetTable vms={filtered} showStorage onDeleted={() => fetchVms(user?.id)} />
        </Card>
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