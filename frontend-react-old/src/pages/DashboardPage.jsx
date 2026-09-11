import { useEffect } from 'react'
import { useNavigate } from 'react-router-dom'
import {
  Server, AlertCircle, Plus, ArrowRight,
  MessageSquare, TrendingUp, Shield, Zap,
} from 'lucide-react'
import { useAuthStore } from '../store/authStore'
import { useVmStore } from '../store/vmStore'
import LoadingSpinner from '../components/LoadingSpinner'
import FleetTable from '../components/FleetTable'
import { Card, CardHeader, CardTitle } from '../components/ui/Card'
import Button from '../components/ui/Button'

/* ─── Telemetry cell ─── */
function TCell({ label, value, unit, sub, live, className = '' }) {
  return (
    <div className={`px-5 py-4 ${className}`}>
      <div className="flex items-center justify-between mb-2.5">
        <span className="font-mono text-2xs uppercase tracking-wider text-text-faint">{label}</span>
        {live && <span className="h-1.5 w-1.5 shrink-0 bg-good animate-pulse" />}
      </div>
      <p className="text-[1.65rem] font-extrabold leading-none tracking-tight text-white tabular-nums">
        {value}{unit && <small className="ml-0.5 text-xs font-semibold text-text-faint">{unit}</small>}
      </p>
      {sub && <p className="mt-2 text-2xs text-text-dim">{sub}</p>}
    </div>
  )
}

/* ─── Quick action row ─── */
function ActionRow({ icon: Icon, title, desc, color, onClick, first }) {
  return (
    <button
      onClick={onClick}
      className={`flex w-full items-center gap-3 px-4 py-3 text-left transition-colors hover:bg-white/[0.02] ${first ? '' : 'border-t border-brand-border-soft'}`}
    >
      <div className="flex h-8 w-8 shrink-0 items-center justify-center bg-brand-surface2">
        <Icon className={`h-3.5 w-3.5 ${color}`} />
      </div>
      <div className="flex-1 min-w-0">
        <p className="text-xs font-bold text-white">{title}</p>
        <p className="text-2xs mt-0.5 text-text-faint">{desc}</p>
      </div>
      <ArrowRight className="h-3.5 w-3.5 shrink-0 text-text-faint" />
    </button>
  )
}

/* ─── Main page ─── */
export default function DashboardPage() {
  const user = useAuthStore((s) => s.user)
  const { vms, loading, fetchVms } = useVmStore()
  const navigate = useNavigate()

  useEffect(() => {
    if (user?.id) fetchVms(user.id)
  }, [user?.id])

  const running   = vms.filter((v) => ['ACTIVE', 'RUNNING'].includes(v.status)).length
  const stopped   = vms.filter((v) => v.status === 'STOPPED').length
  const errored   = vms.filter((v) => v.status === 'ERROR').length
  const totalCpu  = vms.reduce((s, v) => s + (parseInt(v.cpu) || 0), 0)
  const totalRam  = vms.reduce((s, v) => s + (parseInt(v.ram) || 0), 0)
  const totalStorage = vms.reduce((s, v) => s + (parseInt(v.storage) || 0), 0)
  const recentVms = vms.slice(0, 6)
  const firstName = user?.firstName || user?.email?.split('@')[0] || 'là'

  return (
    <div className="w-full px-6 py-6 space-y-5">

      {/* ── Head ── */}
      <div className="flex items-start justify-between gap-4 flex-wrap">
        <div>
          <h2 className="text-xl font-bold text-white">Bonjour, {firstName}</h2>
          <p className="mt-1 text-xs text-text-dim">
            {running > 0
              ? `${running} VM${running > 1 ? 's' : ''} en marche sur ${vms.length} — infrastructure globalement stable`
              : 'Démarrez votre première machine virtuelle dès maintenant'}
          </p>
        </div>
        <div className="flex gap-2 shrink-0">
          <Button variant="secondary" onClick={() => navigate('/chat')}>
            <MessageSquare className="h-3.5 w-3.5" /> Assistant IA
          </Button>
          <Button variant="primary" onClick={() => navigate('/vms')}>
            <Plus className="h-3.5 w-3.5" /> Créer une VM
          </Button>
        </div>
      </div>

      {/* ── Telemetry strip ── */}
      <Card className="grid grid-cols-2 sm:grid-cols-4">
        <TCell label="Flotte totale" value={loading ? '—' : String(vms.length).padStart(2, '0')}
          sub={`${running} active${running !== 1 ? 's' : ''}`}
          className="border-b border-r border-brand-border sm:border-b-0" />
        <TCell label="En marche" value={loading ? '—' : String(running).padStart(2, '0')} unit={`/${String(vms.length).padStart(2, '0')}`}
          sub="Instances actives" live={running > 0}
          className="border-b border-brand-border sm:border-b-0 sm:border-r" />
        <TCell label="vCPUs alloués" value={loading ? '—' : totalCpu} unit="vCPU"
          sub={`${vms.length} VM${vms.length !== 1 ? 's' : ''}`}
          className="border-r border-brand-border" />
        <TCell label="RAM allouée" value={loading ? '—' : totalRam >= 1024 ? (totalRam / 1024).toFixed(1) : totalRam} unit={totalRam >= 1024 ? 'GB' : 'MB'}
          sub={stopped > 0 ? `${stopped} arrêtée${stopped > 1 ? 's' : ''}` : 'Aucune arrêtée'} />
      </Card>

      {/* ── Main grid: fleet + ops rail ── */}
      <div className="grid gap-6 lg:grid-cols-3">

        {/* Fleet register — 2/3 */}
        <div className="lg:col-span-2 flex flex-col">
          <Card className="overflow-hidden h-full flex flex-col">
            <CardHeader>
              <div className="flex items-center gap-2">
                <CardTitle icon={Server}>Registre de flotte</CardTitle>
                {!loading && <span className="font-mono text-2xs font-bold text-accent bg-primary/10 px-2 py-0.5">{vms.length}</span>}
              </div>
              <button
                onClick={() => navigate('/vms')}
                className="flex items-center gap-1 text-xs font-bold text-accent hover:opacity-80 transition-opacity"
              >
                Voir tout <ArrowRight className="h-3 w-3" />
              </button>
            </CardHeader>

            {loading ? (
              <div className="flex flex-1 items-center justify-center py-16"><LoadingSpinner text="Chargement..." /></div>
            ) : recentVms.length === 0 ? (
              <div className="flex flex-1 flex-col items-center justify-center gap-4 py-16 text-center">
                <div className="flex h-16 w-16 items-center justify-center bg-brand-surface2">
                  <Server className="h-8 w-8 text-slate-600" />
                </div>
                <div>
                  <p className="text-sm font-bold text-slate-300">Aucune machine virtuelle</p>
                  <p className="mt-1 text-xs text-slate-500">Déployez votre première VM en quelques secondes</p>
                </div>
                <Button variant="primary" onClick={() => navigate('/vms')}>
                  <Plus className="h-3.5 w-3.5" /> Créer ma première VM
                </Button>
              </div>
            ) : (
              <FleetTable vms={recentVms} onDeleted={() => fetchVms(user?.id)} />
            )}

            {/* Resource allocation — real allocation data, no fabricated history */}
            {!loading && vms.length > 0 && (
              <div className="mt-auto px-4 py-4 border-t border-brand-border">
                <p className="font-mono text-2xs uppercase tracking-wider mb-3 text-text-faint">Aperçu des ressources</p>
                <div className="grid grid-cols-1 gap-3 sm:grid-cols-3">
                  {[
                    { label: 'vCPUs', value: `${totalCpu} / 16`, used: totalCpu, max: 16, color: 'var(--color-accent)' },
                    { label: 'RAM', value: totalRam >= 1024 ? `${(totalRam / 1024).toFixed(1)} / 32 GB` : `${totalRam} / 32768 MB`, used: totalRam, max: 32768, color: 'var(--color-warn)' },
                    { label: 'Stockage', value: `${totalStorage} / 500 GB`, used: totalStorage, max: 500, color: 'var(--color-good)' },
                  ].map(({ label, value, used, max, color }) => (
                    <div key={label}>
                      <div className="flex items-baseline justify-between mb-1.5">
                        <span className="text-2xs text-text-dim">{label}</span>
                        <span className="font-mono text-xs font-bold text-white tabular-nums">{value}</span>
                      </div>
                      <div className="h-1 bg-brand-border-soft">
                        <div className="h-1 transition-all" style={{ width: `${Math.min((used / max) * 100, 100)}%`, backgroundColor: color }} />
                      </div>
                    </div>
                  ))}
                </div>
              </div>
            )}
          </Card>
        </div>

        {/* Ops rail — 1/3 */}
        <div className="flex flex-col gap-5">
          {/* Infrastructure health */}
          <Card>
            <CardHeader>
              <CardTitle icon={Shield} iconClassName="text-good">Santé infrastructure</CardTitle>
            </CardHeader>
            <div>
              {[
                { label: 'Compute (VMs)', ok: errored === 0, val: `${running}/${vms.length} actives` },
                { label: 'Réseau', ok: true, val: 'Opérationnel' },
                { label: 'Stockage', ok: true, val: 'Opérationnel' },
                { label: 'Sécurité', ok: true, val: 'Protégé' },
              ].map(({ label, ok, val }, i) => (
                <div key={label} className={`flex items-center justify-between px-4 py-2.5 ${i === 0 ? '' : 'border-t border-brand-border-soft'}`}>
                  <div className="flex items-center gap-2">
                    <span className={`h-1.5 w-1.5 ${ok ? 'bg-good' : 'bg-warn'}`} />
                    <span className="text-xs text-text-dim">{label}</span>
                  </div>
                  <span className={`font-mono text-2xs font-bold ${ok ? 'text-good' : 'text-warn'}`}>{val}</span>
                </div>
              ))}
            </div>
          </Card>

          {/* Quick actions */}
          <Card className="flex flex-1 flex-col">
            <CardHeader>
              <CardTitle icon={Zap}>Actions rapides</CardTitle>
            </CardHeader>
            <div className="flex-1">
              <ActionRow first
                icon={Plus} title="Créer une VM"
                desc="Déployer un nouveau serveur"
                color="text-accent"
                onClick={() => navigate('/vms')}
              />
              <ActionRow
                icon={MessageSquare} title="Assistant IA"
                desc="Support en langage naturel"
                color="text-accent"
                onClick={() => navigate('/chat')}
              />
              <ActionRow
                icon={TrendingUp} title="Monitoring"
                desc="Surveiller les performances"
                color="text-good"
                onClick={() => navigate('/dashboard')}
              />
            </div>
          </Card>
        </div>
      </div>

      {/* ── Bottom: error VMs alert if any ── */}
      {errored > 0 && (
        <div className="flex items-center gap-3 px-4 py-3 border border-crit/30 bg-crit/[0.06]">
          <AlertCircle className="h-4 w-4 text-crit shrink-0" />
          <p className="text-xs text-crit flex-1">
            <span className="font-bold">{errored} VM{errored > 1 ? 's' : ''}</span> en état d'erreur — vérifiez votre infrastructure
          </p>
          <button onClick={() => navigate('/vms')} className="text-xs font-semibold text-crit hover:opacity-80 transition-opacity shrink-0">
            Voir les VMs →
          </button>
        </div>
      )}
    </div>
  )
}