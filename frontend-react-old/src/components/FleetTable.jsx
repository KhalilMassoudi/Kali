import { useState } from 'react'
import { RotateCcw, Square, Trash2, Loader2, Check, X } from 'lucide-react'
import { useVmStore } from '../store/vmStore'
import Badge from './ui/Badge'
import Button from './ui/Button'

const STATUS_CONFIG = {
  ACTIVE:  { variant: 'good',    label: 'En marche' },
  RUNNING: { variant: 'good',    label: 'En marche' },
  STOPPED: { variant: 'warn',    label: 'Arrêtée' },
  PENDING: { variant: 'primary', label: 'En cours' },
  ERROR:   { variant: 'crit',    label: 'Erreur' },
  DELETED: { variant: 'neutral', label: 'Supprimée' },
}

function formatRam(ram) {
  const v = parseInt(ram) || 0
  return v >= 1024 ? `${v / 1024} GB` : `${v} MB`
}

const th = 'px-4 py-2.5 text-left font-mono text-2xs font-medium uppercase tracking-wider text-text-faint'
const td = 'px-4 py-3'
const iconBtn = 'border border-brand-border'

function useRowActions(vm, onDeleted) {
  const [confirmDelete, setConfirmDelete] = useState(false)
  const [actionLoading, setActionLoading] = useState(null)
  const { restartVm, stopVm, deleteVm } = useVmStore()

  const run = async (action, fn) => {
    setActionLoading(action)
    try { await fn() } finally { setActionLoading(null) }
  }

  return {
    confirmDelete, setConfirmDelete, actionLoading,
    onStop: () => run('stop', () => stopVm(vm.id)),
    onRestart: () => run('restart', () => restartVm(vm.id)),
    onDelete: () => run('delete', async () => { await deleteVm(vm.id); onDeleted?.() }),
  }
}

function RowActionButtons({ vm, actions }) {
  const { confirmDelete, setConfirmDelete, actionLoading, onStop, onRestart, onDelete } = actions

  if (confirmDelete) {
    return (
      <div className="flex items-center justify-end gap-1.5">
        <Button variant="danger" size="sm" onClick={onDelete} disabled={actionLoading === 'delete'}>
          {actionLoading === 'delete' ? <Loader2 className="h-3 w-3 animate-spin" /> : <Check className="h-3 w-3" />} Confirmer
        </Button>
        <Button variant="outline" size="icon" onClick={() => setConfirmDelete(false)}>
          <X className="h-3 w-3" />
        </Button>
      </div>
    )
  }

  return (
    <div className="flex items-center justify-end gap-1">
      {vm.status !== 'STOPPED' && vm.status !== 'DELETED' && (
        <Button
          variant="outline"
          size="icon"
          className={`${iconBtn} hover:text-warn`}
          onClick={onStop}
          disabled={!!actionLoading}
          title="Arrêter"
        >
          {actionLoading === 'stop' ? <Loader2 className="h-3 w-3 animate-spin" /> : <Square className="h-3 w-3" />}
        </Button>
      )}
      {vm.status !== 'DELETED' && (
        <Button
          variant="outline"
          size="icon"
          className={`${iconBtn} hover:text-primary`}
          onClick={onRestart}
          disabled={!!actionLoading}
          title="Redémarrer"
        >
          {actionLoading === 'restart' ? <Loader2 className="h-3 w-3 animate-spin" /> : <RotateCcw className="h-3 w-3" />}
        </Button>
      )}
      <Button
        variant="outline"
        size="icon"
        className={`${iconBtn} hover:text-crit`}
        onClick={() => setConfirmDelete(true)}
        disabled={!!actionLoading}
        title="Supprimer"
      >
        <Trash2 className="h-3 w-3" />
      </Button>
    </div>
  )
}

function Row({ vm, showStorage, onDeleted }) {
  const actions = useRowActions(vm, onDeleted)
  const status = STATUS_CONFIG[vm.status] || STATUS_CONFIG.PENDING
  const live = vm.status === 'ACTIVE' || vm.status === 'RUNNING'
  const dim = vm.status === 'STOPPED' || vm.status === 'ERROR' || vm.status === 'DELETED'

  return (
    <tr className="border-b border-brand-border-soft transition-colors hover:bg-white/[0.015]">
      <td className={td}>
        <p className="text-sm font-bold leading-tight text-white">{vm.name}</p>
        <p className="mt-0.5 text-2xs text-text-faint">{vm.os || 'N/A'}</p>
      </td>
      <td className={td}>
        <Badge variant={status.variant} dot={live}>{status.label}</Badge>
      </td>
      <td className={`${td} font-mono text-xs ${dim ? 'text-slate-600' : 'text-slate-300'}`}>{vm.cpu} vCPU</td>
      <td className={`${td} font-mono text-xs ${dim ? 'text-slate-600' : 'text-slate-300'}`}>{formatRam(vm.ram)}</td>
      {showStorage && (
        <td className={`${td} font-mono text-xs ${dim ? 'text-slate-600' : 'text-slate-300'}`}>{vm.storage} GB</td>
      )}
      <td className={`${td} font-mono text-xs ${vm.ip ? 'text-accent' : 'text-slate-600'}`}>{vm.ip || '—'}</td>
      <td className={td}>
        <RowActionButtons vm={vm} actions={actions} />
      </td>
    </tr>
  )
}

function RowCard({ vm, showStorage, onDeleted }) {
  const actions = useRowActions(vm, onDeleted)
  const status = STATUS_CONFIG[vm.status] || STATUS_CONFIG.PENDING
  const live = vm.status === 'ACTIVE' || vm.status === 'RUNNING'
  const dim = vm.status === 'STOPPED' || vm.status === 'ERROR' || vm.status === 'DELETED'

  return (
    <div className="border-b border-brand-border-soft px-4 py-3.5 last:border-b-0">
      <div className="flex items-start justify-between gap-3">
        <div className="min-w-0">
          <p className="truncate text-sm font-bold leading-tight text-white">{vm.name}</p>
          <p className="mt-0.5 text-2xs text-text-faint">{vm.os || 'N/A'}</p>
        </div>
        <Badge variant={status.variant} dot={live} className="shrink-0">{status.label}</Badge>
      </div>

      <div className="mt-3 flex flex-wrap gap-x-4 gap-y-1 font-mono text-xs">
        <span className={dim ? 'text-slate-600' : 'text-slate-300'}>{vm.cpu} vCPU</span>
        <span className={dim ? 'text-slate-600' : 'text-slate-300'}>{formatRam(vm.ram)}</span>
        {showStorage && <span className={dim ? 'text-slate-600' : 'text-slate-300'}>{vm.storage} GB</span>}
        <span className={vm.ip ? 'text-accent' : 'text-slate-600'}>{vm.ip || '—'}</span>
      </div>

      <div className="mt-3">
        <RowActionButtons vm={vm} actions={actions} />
      </div>
    </div>
  )
}

export default function FleetTable({ vms, showStorage = false, onDeleted }) {
  return (
    <>
      {/* Desktop / tablet: table */}
      <div className="hidden overflow-x-auto md:block">
        <table className="w-full border-collapse">
          <thead>
            <tr className="border-b border-brand-border">
              <th className={th}>Machine</th>
              <th className={th}>Statut</th>
              <th className={th}>CPU</th>
              <th className={th}>RAM</th>
              {showStorage && <th className={th}>Stockage</th>}
              <th className={th}>Adresse IP</th>
              <th className="px-4 py-2.5"></th>
            </tr>
          </thead>
          <tbody>
            {vms.map((vm) => <Row key={vm.id} vm={vm} showStorage={showStorage} onDeleted={onDeleted} />)}
          </tbody>
        </table>
      </div>

      {/* Mobile: stacked cards — table columns don't fit narrow viewports */}
      <div className="md:hidden">
        {vms.map((vm) => <RowCard key={vm.id} vm={vm} showStorage={showStorage} onDeleted={onDeleted} />)}
      </div>
    </>
  )
}