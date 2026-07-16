import { useState } from 'react'
import { Server, Cpu, HardDrive, MemoryStick, RotateCcw, Square, Trash2, Globe, Loader2 } from 'lucide-react'
import { useVmStore } from '../store/vmStore'

const STATUS_CONFIG = {
  ACTIVE:  { color: 'text-green-400',  bg: 'bg-green-400/10',  dot: 'bg-green-400',  label: 'Actif',     glow: '#10B981' },
  RUNNING: { color: 'text-green-400',  bg: 'bg-green-400/10',  dot: 'bg-green-400',  label: 'En marche', glow: '#10B981' },
  STOPPED: { color: 'text-yellow-400', bg: 'bg-yellow-400/10', dot: 'bg-yellow-400', label: 'Arrêté',    glow: '#F59E0B' },
  PENDING: { color: 'text-blue-400',   bg: 'bg-blue-400/10',   dot: 'bg-blue-400',   label: 'En cours',  glow: '#0EA5E9' },
  ERROR:   { color: 'text-red-400',    bg: 'bg-red-400/10',    dot: 'bg-red-400',    label: 'Erreur',    glow: '#EF4444' },
  DELETED: { color: 'text-slate-400',  bg: 'bg-slate-400/10',  dot: 'bg-slate-400',  label: 'Supprimé',  glow: '#6B7280' },
}

export default function VmCard({ vm, onDeleted, compact = false }) {
  const [confirmDelete, setConfirmDelete] = useState(false)
  const [actionLoading, setActionLoading] = useState(null)
  const { restartVm, stopVm, deleteVm } = useVmStore()

  const status = STATUS_CONFIG[vm.status] || STATUS_CONFIG.PENDING
  const ram = parseInt(vm.ram) >= 1024 ? `${parseInt(vm.ram) / 1024} GB` : `${vm.ram} MB`

  const handle = async (action, fn) => {
    setActionLoading(action)
    try { await fn() } finally { setActionLoading(null) }
  }

  if (confirmDelete) {
    return (
      <div
        className="rounded-xl p-4 flex flex-col gap-4"
        style={{ border: '1px solid rgba(239,68,68,0.4)', backgroundColor: 'rgba(239,68,68,0.05)' }}
      >
        <div>
          <p className="text-sm font-bold text-white mb-1">Supprimer cette VM ?</p>
          <p className="text-xs text-slate-400">
            <span className="font-semibold text-white">{vm.name}</span> sera supprimée définitivement.
          </p>
        </div>
        <div className="flex gap-2">
          <button
            onClick={() => handle('delete', async () => { await deleteVm(vm.id); onDeleted?.() })}
            disabled={actionLoading === 'delete'}
            className="flex items-center gap-1.5 rounded-lg bg-red-600 px-4 py-2 text-xs font-bold text-white hover:bg-red-700 disabled:opacity-50 transition-colors"
          >
            {actionLoading === 'delete' ? <Loader2 className="h-3.5 w-3.5 animate-spin" /> : <Trash2 className="h-3.5 w-3.5" />}
            Supprimer
          </button>
          <button
            onClick={() => setConfirmDelete(false)}
            className="rounded-lg px-4 py-2 text-xs font-semibold text-slate-400 hover:text-white transition-colors"
            style={{ border: '1px solid rgba(26,51,84,0.8)' }}
          >
            Annuler
          </button>
        </div>
      </div>
    )
  }

  return (
    <div
      className="rounded-xl p-4 flex flex-col gap-3 transition-all hover:-translate-y-0.5"
      style={{ border: '1px solid rgba(26,51,84,0.8)', backgroundColor: '#0E1825' }}
      onMouseEnter={(e) => e.currentTarget.style.boxShadow = `0 6px 24px ${status.glow}18`}
      onMouseLeave={(e) => e.currentTarget.style.boxShadow = 'none'}
    >
      {/* Header */}
      <div className="flex items-start justify-between gap-2">
        <div className="flex items-center gap-2.5 min-w-0">
          <div className="flex h-8 w-8 items-center justify-center rounded-lg bg-primary/10 shrink-0">
            <Server className="h-4 w-4 text-primary" />
          </div>
          <div className="min-w-0">
            <h3 className="text-sm font-bold text-white leading-tight truncate">{vm.name}</h3>
            <p className="text-[10px] text-slate-500 truncate">{vm.os || 'N/A'}</p>
          </div>
        </div>
        <span className={`flex items-center gap-1 rounded-full px-2 py-0.5 text-[10px] font-bold ${status.bg} ${status.color} shrink-0`}>
          <span className={`h-1 w-1 rounded-full ${status.dot} animate-pulse`} />
          {status.label}
        </span>
      </div>

      {/* Specs — 2x2 grid */}
      <div className="grid grid-cols-2 gap-1.5">
        <div className="rounded-lg px-3 py-2" style={{ backgroundColor: '#131F2E' }}>
          <p className="text-[10px] text-slate-600 flex items-center gap-1 mb-0.5">
            <Cpu className="h-2.5 w-2.5" /> CPU
          </p>
          <p className="text-xs font-bold text-white">{vm.cpu} vCPU</p>
        </div>
        <div className="rounded-lg px-3 py-2" style={{ backgroundColor: '#131F2E' }}>
          <p className="text-[10px] text-slate-600 flex items-center gap-1 mb-0.5">
            <MemoryStick className="h-2.5 w-2.5" /> RAM
          </p>
          <p className="text-xs font-bold text-white">{ram}</p>
        </div>
        <div className="rounded-lg px-3 py-2" style={{ backgroundColor: '#131F2E' }}>
          <p className="text-[10px] text-slate-600 flex items-center gap-1 mb-0.5">
            <HardDrive className="h-2.5 w-2.5" /> Disque
          </p>
          <p className="text-xs font-bold text-white">{vm.storage} GB</p>
        </div>
        <div className="rounded-lg px-3 py-2" style={{ backgroundColor: '#131F2E' }}>
          <p className="text-[10px] text-slate-600 flex items-center gap-1 mb-0.5">
            <Globe className="h-2.5 w-2.5" /> IP
          </p>
          <p className={`text-xs font-mono font-bold truncate ${vm.ip ? 'text-accent' : 'text-slate-600'}`}>
            {vm.ip || '—'}
          </p>
        </div>
      </div>

      {/* Actions */}
      {!compact && (
        <div className="flex gap-1.5">
          {vm.status !== 'STOPPED' && vm.status !== 'DELETED' && (
            <button
              onClick={() => handle('stop', () => stopVm(vm.id))}
              disabled={!!actionLoading}
              className="flex flex-1 items-center justify-center gap-1 rounded-lg py-1.5 text-xs font-semibold text-yellow-400 hover:bg-yellow-400/10 disabled:opacity-50 transition-colors"
              style={{ border: '1px solid rgba(234,179,8,0.25)' }}
            >
              {actionLoading === 'stop' ? <Loader2 className="h-3 w-3 animate-spin" /> : <Square className="h-3 w-3" />}
              Arrêter
            </button>
          )}
          {vm.status !== 'DELETED' && (
            <button
              onClick={() => handle('restart', () => restartVm(vm.id))}
              disabled={!!actionLoading}
              className="flex flex-1 items-center justify-center gap-1 rounded-lg py-1.5 text-xs font-semibold text-primary hover:bg-primary/10 disabled:opacity-50 transition-colors"
              style={{ border: '1px solid rgba(14,165,233,0.25)' }}
            >
              {actionLoading === 'restart' ? <Loader2 className="h-3 w-3 animate-spin" /> : <RotateCcw className="h-3 w-3" />}
              Restart
            </button>
          )}
          <button
            onClick={() => setConfirmDelete(true)}
            disabled={!!actionLoading}
            className="flex items-center justify-center rounded-lg px-2.5 py-1.5 text-red-400 hover:bg-red-400/10 disabled:opacity-50 transition-colors"
            style={{ border: '1px solid rgba(239,68,68,0.25)' }}
            title="Supprimer"
          >
            <Trash2 className="h-3 w-3" />
          </button>
        </div>
      )}
    </div>
  )
}