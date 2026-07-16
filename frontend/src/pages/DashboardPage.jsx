import { useEffect } from 'react'
import { useNavigate } from 'react-router-dom'
import {
  Server, Activity, Square, AlertCircle, Plus, ArrowRight,
  MessageSquare, Cpu, HardDrive, TrendingUp, Zap, Shield, Globe,
} from 'lucide-react'
import { useAuthStore } from '../store/authStore'
import { useVmStore } from '../store/vmStore'
import LoadingSpinner from '../components/LoadingSpinner'
import VmCard from '../components/VmCard'

/* ─── Stat card ─── */
function StatCard({ icon: Icon, label, value, sub, color, bg }) {
  return (
    <div
      className="rounded-xl p-4 flex items-center gap-4 transition-all hover:translate-y-[-1px]"
      style={{ border: '1px solid rgba(26,51,84,0.8)', backgroundColor: '#0E1825' }}
    >
      <div className={`flex h-10 w-10 shrink-0 items-center justify-center rounded-xl ${bg}`}>
        <Icon className={`h-5 w-5 ${color}`} />
      </div>
      <div className="min-w-0">
        <p className="text-2xl font-bold text-white leading-none">{value}</p>
        <p className="text-xs text-slate-500 mt-1">{label}</p>
        {sub && <p className="text-[10px] text-slate-600 mt-0.5">{sub}</p>}
      </div>
    </div>
  )
}

/* ─── Quick action card ─── */
function ActionCard({ icon: Icon, title, desc, color, bg, onClick }) {
  return (
    <button
      onClick={onClick}
      className="group flex items-start gap-3 rounded-xl p-4 text-left w-full transition-all hover:translate-y-[-1px]"
      style={{ border: '1px solid rgba(26,51,84,0.8)', backgroundColor: '#0E1825' }}
      onMouseEnter={(e) => e.currentTarget.style.borderColor = color + '40'}
      onMouseLeave={(e) => e.currentTarget.style.borderColor = 'rgba(26,51,84,0.8)'}
    >
      <div className={`flex h-9 w-9 shrink-0 items-center justify-center rounded-lg ${bg}`}>
        <Icon className={`h-4 w-4 ${color}`} />
      </div>
      <div className="flex-1 min-w-0">
        <p className="text-sm font-semibold text-white">{title}</p>
        <p className="text-xs text-slate-500 mt-0.5 leading-relaxed">{desc}</p>
      </div>
      <ArrowRight className="h-3.5 w-3.5 text-slate-600 group-hover:text-slate-400 transition-colors shrink-0 mt-0.5" />
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
  const recentVms = vms.slice(0, 6)
  const firstName = user?.firstName || user?.email?.split('@')[0] || 'là'

  return (
    <div className="w-full px-5 py-5 space-y-5">

      {/* ── Top welcome + CTA ── */}
      <div
        className="relative overflow-hidden rounded-2xl px-6 py-5 flex items-center justify-between gap-4 flex-wrap"
        style={{ background: 'linear-gradient(135deg, #0E1825 0%, #0A1F35 60%, #071525 100%)', border: '1px solid rgba(14,165,233,0.2)' }}
      >
        {/* Glow blob */}
        <div className="pointer-events-none absolute -right-20 -top-20 h-64 w-64 rounded-full opacity-20"
          style={{ background: 'radial-gradient(circle, #0EA5E9, transparent)' }} />

        <div>
          <p className="text-xs font-semibold text-primary uppercase tracking-widest mb-1">Bienvenue</p>
          <h1 className="text-2xl font-bold text-white">Bonjour, {firstName} 👋</h1>
          <p className="text-sm text-slate-400 mt-1">
            {running > 0
              ? `${running} VM${running > 1 ? 's' : ''} active${running > 1 ? 's' : ''} · Infrastructure opérationnelle`
              : 'Démarrez votre première machine virtuelle dès maintenant'}
          </p>
        </div>
        <div className="flex gap-2 shrink-0">
          <button
            onClick={() => navigate('/chat')}
            className="flex items-center gap-1.5 rounded-lg px-4 py-2 text-xs font-semibold text-primary hover:bg-primary/10 transition-colors"
            style={{ border: '1px solid rgba(14,165,233,0.3)' }}
          >
            <MessageSquare className="h-3.5 w-3.5" /> Assistant IA
          </button>
          <button
            onClick={() => navigate('/vms')}
            className="flex items-center gap-1.5 rounded-lg px-4 py-2 text-xs font-bold text-white transition-all hover:opacity-90"
            style={{ background: 'linear-gradient(135deg, #0EA5E9, #38BDF8)', boxShadow: '0 4px 16px rgba(14,165,233,0.3)' }}
          >
            <Plus className="h-3.5 w-3.5" /> Créer une VM
          </button>
        </div>
      </div>

      {/* ── Stats row ── */}
      <div className="grid grid-cols-2 gap-3 sm:grid-cols-4">
        <StatCard icon={Server}      label="Total VMs"  value={loading ? '—' : vms.length}
          sub={`${running} active${running !== 1 ? 's' : ''}`}
          color="text-primary"     bg="bg-primary/10" />
        <StatCard icon={Activity}    label="En marche"  value={loading ? '—' : running}
          sub="Instances actives"
          color="text-green-400"   bg="bg-green-400/10" />
        <StatCard icon={Cpu}         label="vCPUs total" value={loading ? '—' : totalCpu}
          sub={`${vms.length} VM${vms.length !== 1 ? 's' : ''}`}
          color="text-violet-400"  bg="bg-violet-400/10" />
        <StatCard icon={HardDrive}   label="RAM totale"  value={loading ? '—' : totalRam >= 1024 ? `${(totalRam/1024).toFixed(0)} GB` : `${totalRam} MB`}
          sub={stopped > 0 ? `${stopped} arrêtée${stopped > 1 ? 's' : ''}` : 'Aucune arrêtée'}
          color="text-yellow-400"  bg="bg-yellow-400/10" />
      </div>

      {/* ── Main grid: VMs + Quick actions ── */}
      <div className="grid gap-4 lg:grid-cols-3">

        {/* VMs list — 2/3 */}
        <div className="lg:col-span-2">
          <div className="rounded-xl overflow-hidden h-full" style={{ border: '1px solid rgba(26,51,84,0.8)', backgroundColor: '#0E1825' }}>
            <div className="flex items-center justify-between px-5 py-3.5" style={{ borderBottom: '1px solid rgba(26,51,84,0.8)' }}>
              <div className="flex items-center gap-2">
                <Server className="h-4 w-4 text-primary" />
                <span className="text-sm font-bold text-white">Mes VMs</span>
                {!loading && <span className="rounded-full bg-primary/10 px-2 py-0.5 text-[10px] font-bold text-primary">{vms.length}</span>}
              </div>
              <button
                onClick={() => navigate('/vms')}
                className="flex items-center gap-1 text-xs font-semibold text-primary hover:text-accent transition-colors"
              >
                Voir tout <ArrowRight className="h-3 w-3" />
              </button>
            </div>

            <div className="p-4">
              {loading ? (
                <div className="flex justify-center py-12"><LoadingSpinner text="Chargement..." /></div>
              ) : recentVms.length === 0 ? (
                <div className="flex flex-col items-center gap-4 py-10 text-center">
                  <div className="flex h-16 w-16 items-center justify-center rounded-2xl" style={{ backgroundColor: '#131F2E' }}>
                    <Server className="h-8 w-8 text-slate-600" />
                  </div>
                  <div>
                    <p className="text-sm font-bold text-slate-300">Aucune machine virtuelle</p>
                    <p className="mt-1 text-xs text-slate-500">Déployez votre première VM en quelques secondes</p>
                  </div>
                  <button
                    onClick={() => navigate('/vms')}
                    className="flex items-center gap-1.5 rounded-lg px-4 py-2 text-xs font-bold text-white"
                    style={{ background: 'linear-gradient(135deg, #0EA5E9, #38BDF8)', boxShadow: '0 4px 16px rgba(14,165,233,0.25)' }}
                  >
                    <Plus className="h-3.5 w-3.5" /> Créer ma première VM
                  </button>
                </div>
              ) : (
                <div className="grid gap-3 sm:grid-cols-2">
                  {recentVms.map((vm) => <VmCard key={vm.id} vm={vm} compact onDeleted={() => fetchVms(user?.id)} />)}
                </div>
              )}
            </div>
          </div>
        </div>

        {/* Quick actions sidebar — 1/3 */}
        <div className="flex flex-col gap-3">
          {/* Infrastructure health */}
          <div className="rounded-xl p-4" style={{ border: '1px solid rgba(26,51,84,0.8)', backgroundColor: '#0E1825' }}>
            <div className="flex items-center gap-2 mb-3">
              <Shield className="h-4 w-4 text-green-400" />
              <span className="text-xs font-bold text-white">Santé Infrastructure</span>
            </div>
            <div className="space-y-2.5">
              {[
                { label: 'Compute (VMs)', ok: errored === 0, val: `${running}/${vms.length} actives` },
                { label: 'Réseau', ok: true, val: 'Opérationnel' },
                { label: 'Stockage', ok: true, val: 'Opérationnel' },
                { label: 'Sécurité', ok: true, val: 'Protégé' },
              ].map(({ label, ok, val }) => (
                <div key={label} className="flex items-center justify-between">
                  <div className="flex items-center gap-2">
                    <span className={`h-1.5 w-1.5 rounded-full ${ok ? 'bg-green-400' : 'bg-red-400'}`} />
                    <span className="text-xs text-slate-400">{label}</span>
                  </div>
                  <span className={`text-[10px] font-semibold ${ok ? 'text-green-400' : 'text-red-400'}`}>{val}</span>
                </div>
              ))}
            </div>
          </div>

          {/* Quick actions */}
          <div className="rounded-xl p-4" style={{ border: '1px solid rgba(26,51,84,0.8)', backgroundColor: '#0E1825' }}>
            <p className="text-xs font-bold text-white mb-3">Actions rapides</p>
            <div className="space-y-1.5">
              <ActionCard
                icon={Plus} title="Créer une VM"
                desc="Déployer un nouveau serveur"
                color="text-primary" bg="bg-primary/10"
                onClick={() => navigate('/vms')}
              />
              <ActionCard
                icon={MessageSquare} title="Assistant IA"
                desc="Support en langage naturel"
                color="text-violet-400" bg="bg-violet-400/10"
                onClick={() => navigate('/chat')}
              />
              <ActionCard
                icon={TrendingUp} title="Monitoring"
                desc="Surveiller les performances"
                color="text-green-400" bg="bg-green-400/10"
                onClick={() => navigate('/dashboard')}
              />
            </div>
          </div>

          {/* Usage summary */}
          {vms.length > 0 && (
            <div className="rounded-xl p-4" style={{ border: '1px solid rgba(26,51,84,0.8)', backgroundColor: '#0E1825' }}>
              <p className="text-xs font-bold text-white mb-3">Résumé utilisation</p>
              <div className="space-y-3">
                <div>
                  <div className="flex justify-between mb-1">
                    <span className="text-[10px] text-slate-500">VMs actives</span>
                    <span className="text-[10px] font-bold text-white">{running}/{vms.length}</span>
                  </div>
                  <div className="h-1 rounded-full" style={{ backgroundColor: '#131F2E' }}>
                    <div className="h-1 rounded-full bg-primary transition-all" style={{ width: vms.length ? `${(running/vms.length)*100}%` : '0%' }} />
                  </div>
                </div>
                <div>
                  <div className="flex justify-between mb-1">
                    <span className="text-[10px] text-slate-500">CPU alloués</span>
                    <span className="text-[10px] font-bold text-white">{totalCpu} vCPUs</span>
                  </div>
                  <div className="h-1 rounded-full" style={{ backgroundColor: '#131F2E' }}>
                    <div className="h-1 rounded-full bg-violet-400" style={{ width: `${Math.min((totalCpu/16)*100, 100)}%` }} />
                  </div>
                </div>
                <div>
                  <div className="flex justify-between mb-1">
                    <span className="text-[10px] text-slate-500">RAM allouée</span>
                    <span className="text-[10px] font-bold text-white">
                      {totalRam >= 1024 ? `${(totalRam/1024).toFixed(1)} GB` : `${totalRam} MB`}
                    </span>
                  </div>
                  <div className="h-1 rounded-full" style={{ backgroundColor: '#131F2E' }}>
                    <div className="h-1 rounded-full bg-yellow-400" style={{ width: `${Math.min((totalRam/32768)*100, 100)}%` }} />
                  </div>
                </div>
              </div>
            </div>
          )}
        </div>
      </div>

      {/* ── Bottom: error VMs alert if any ── */}
      {errored > 0 && (
        <div
          className="flex items-center gap-3 rounded-xl px-4 py-3"
          style={{ border: '1px solid rgba(239,68,68,0.3)', backgroundColor: 'rgba(239,68,68,0.06)' }}
        >
          <AlertCircle className="h-4 w-4 text-red-400 shrink-0" />
          <p className="text-xs text-red-300 flex-1">
            <span className="font-bold">{errored} VM{errored > 1 ? 's' : ''}</span> en état d'erreur — vérifiez votre infrastructure
          </p>
          <button onClick={() => navigate('/vms')} className="text-xs font-semibold text-red-400 hover:text-red-300 transition-colors shrink-0">
            Voir les VMs →
          </button>
        </div>
      )}
    </div>
  )
}
