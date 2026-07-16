import { useState } from 'react'
import { User, Mail, Shield, Save, Loader2, CheckCircle, Key, Bell, Lock } from 'lucide-react'
import { useAuthStore } from '../store/authStore'

const TABS = [
  { key: 'profile',  label: 'Profil',   icon: User },
  { key: 'security', label: 'Sécurité', icon: Shield },
]

export default function SettingsPage() {
  const user = useAuthStore((s) => s.user)
  const [tab, setTab] = useState('profile')
  const [saved, setSaved] = useState(false)
  const [loading, setLoading] = useState(false)

  const handleSave = async (e) => {
    e.preventDefault()
    setLoading(true)
    setTimeout(() => {
      setLoading(false)
      setSaved(true)
      setTimeout(() => setSaved(false), 3000)
    }, 800)
  }

  const inputClass = 'w-full rounded-xl px-4 py-3 text-sm text-white placeholder-slate-600 focus:outline-none transition-all'
  const inputStyle = { border: '1px solid rgba(26,51,84,0.8)', backgroundColor: '#131F2E' }
  const disabledStyle = { border: '1px solid rgba(26,51,84,0.5)', backgroundColor: '#0E1825', opacity: 0.6 }
  const labelClass = 'block text-xs font-semibold text-slate-400 mb-1.5'

  const displayName = user ? `${user.firstName || ''} ${user.lastName || ''}`.trim() : ''
  const initials = displayName.split(' ').map((w) => w[0]).join('').toUpperCase().slice(0, 2)

  return (
    <div className="w-full px-5 py-5">
      {/* Page header */}
      <div className="mb-5">
        <h2 className="text-xl font-bold text-white">Paramètres</h2>
        <p className="mt-0.5 text-xs text-slate-500">Gérez votre compte et vos préférences</p>
      </div>

      {/* Tabs */}
      <div
        className="flex gap-1 mb-5 rounded-xl p-1"
        style={{ backgroundColor: '#0E1825', border: '1px solid rgba(26,51,84,0.8)' }}
      >
        {TABS.map(({ key, label, icon: Icon }) => (
          <button
            key={key}
            onClick={() => setTab(key)}
            className={`flex flex-1 items-center justify-center gap-2 rounded-lg py-2 text-xs font-bold transition-all ${
              tab === key ? 'text-white' : 'text-slate-500 hover:text-slate-300'
            }`}
            style={tab === key ? { background: 'linear-gradient(135deg, #0EA5E9, #0284C7)', boxShadow: '0 2px 8px rgba(14,165,233,0.2)' } : {}}
          >
            <Icon className="h-3.5 w-3.5" /> {label}
          </button>
        ))}
      </div>

      {/* Profile tab */}
      {tab === 'profile' && (
        <div className="rounded-xl overflow-hidden" style={{ border: '1px solid rgba(26,51,84,0.8)', backgroundColor: '#0E1825' }}>
          {/* Avatar section */}
          <div className="p-5" style={{ borderBottom: '1px solid rgba(26,51,84,0.8)' }}>
            <div className="flex items-center gap-4">
              <div
                className="flex h-16 w-16 shrink-0 items-center justify-center rounded-2xl text-2xl font-bold text-white"
                style={{ background: 'linear-gradient(135deg, #0EA5E9, #38BDF8)', boxShadow: '0 0 24px rgba(14,165,233,0.2)' }}
              >
                {initials || '?'}
              </div>
              <div>
                <h3 className="text-base font-bold text-white">{displayName || 'Utilisateur'}</h3>
                <p className="text-xs text-slate-500 mt-0.5">{user?.email}</p>
                <div className="mt-2 flex gap-2">
                  <span
                    className="inline-flex items-center rounded-full px-2.5 py-0.5 text-[10px] font-bold text-primary"
                    style={{ border: '1px solid rgba(14,165,233,0.3)', backgroundColor: 'rgba(14,165,233,0.08)' }}
                  >
                    {user?.role || 'USER'}
                  </span>
                  <span
                    className="inline-flex items-center gap-1 rounded-full px-2.5 py-0.5 text-[10px] font-bold text-green-400"
                    style={{ border: '1px solid rgba(34,197,94,0.3)', backgroundColor: 'rgba(34,197,94,0.08)' }}
                  >
                    <span className="h-1 w-1 rounded-full bg-green-400 animate-pulse" /> Actif
                  </span>
                </div>
              </div>
            </div>
          </div>

          <form onSubmit={handleSave} className="p-5 space-y-4">
            <div className="grid grid-cols-2 gap-3">
              <div>
                <label className={labelClass}>Prénom</label>
                <input
                  className={inputClass}
                  style={inputStyle}
                  defaultValue={user?.firstName || ''}
                  placeholder="Jean"
                  onFocus={(e) => e.target.style.borderColor = 'rgba(14,165,233,0.5)'}
                  onBlur={(e) => e.target.style.borderColor = 'rgba(26,51,84,0.8)'}
                />
              </div>
              <div>
                <label className={labelClass}>Nom</label>
                <input
                  className={inputClass}
                  style={inputStyle}
                  defaultValue={user?.lastName || ''}
                  placeholder="Dupont"
                  onFocus={(e) => e.target.style.borderColor = 'rgba(14,165,233,0.5)'}
                  onBlur={(e) => e.target.style.borderColor = 'rgba(26,51,84,0.8)'}
                />
              </div>
            </div>

            <div>
              <label className={`${labelClass} flex items-center gap-1.5`}>
                <Mail className="h-3 w-3" /> Adresse email
              </label>
              <input type="email" className={inputClass} style={disabledStyle} defaultValue={user?.email || ''} disabled />
              <p className="mt-1 text-[10px] text-slate-600">L'adresse email ne peut pas être modifiée</p>
            </div>

            <div className="flex items-center justify-between pt-1">
              <p className="text-[10px] text-slate-700 font-mono">
                ID: {user?.id || '—'}
              </p>
              <button
                type="submit"
                disabled={loading}
                className="flex items-center gap-2 rounded-lg px-5 py-2 text-xs font-bold text-white transition-all disabled:opacity-60 hover:opacity-90"
                style={{ background: 'linear-gradient(135deg, #0EA5E9, #38BDF8)', boxShadow: '0 4px 16px rgba(14,165,233,0.25)' }}
              >
                {loading ? (
                  <><Loader2 className="h-3.5 w-3.5 animate-spin" /> Enregistrement...</>
                ) : saved ? (
                  <><CheckCircle className="h-3.5 w-3.5" /> Enregistré !</>
                ) : (
                  <><Save className="h-3.5 w-3.5" /> Enregistrer</>
                )}
              </button>
            </div>
          </form>
        </div>
      )}

      {/* Security tab */}
      {tab === 'security' && (
        <div className="space-y-4">
          <div className="rounded-xl overflow-hidden" style={{ border: '1px solid rgba(26,51,84,0.8)', backgroundColor: '#0E1825' }}>
            <div className="flex items-center gap-3 px-5 py-4" style={{ borderBottom: '1px solid rgba(26,51,84,0.8)' }}>
              <div
                className="flex h-9 w-9 items-center justify-center rounded-xl"
                style={{ backgroundColor: 'rgba(245,158,11,0.1)', border: '1px solid rgba(245,158,11,0.2)' }}
              >
                <Key className="h-4 w-4 text-yellow-400" />
              </div>
              <div>
                <h3 className="text-sm font-bold text-white">Changer le mot de passe</h3>
                <p className="text-[10px] text-slate-500 mt-0.5">Mettez à jour votre mot de passe régulièrement</p>
              </div>
            </div>
            <div className="p-5 space-y-4">
              <div>
                <label className={labelClass}>Mot de passe actuel</label>
                <input
                  type="password"
                  className={inputClass}
                  style={inputStyle}
                  placeholder="••••••••"
                  onFocus={(e) => e.target.style.borderColor = 'rgba(14,165,233,0.5)'}
                  onBlur={(e) => e.target.style.borderColor = 'rgba(26,51,84,0.8)'}
                />
              </div>
              <div>
                <label className={labelClass}>Nouveau mot de passe</label>
                <input
                  type="password"
                  className={inputClass}
                  style={inputStyle}
                  placeholder="Min. 6 caractères"
                  onFocus={(e) => e.target.style.borderColor = 'rgba(14,165,233,0.5)'}
                  onBlur={(e) => e.target.style.borderColor = 'rgba(26,51,84,0.8)'}
                />
              </div>
              <div>
                <label className={labelClass}>Confirmer le mot de passe</label>
                <input
                  type="password"
                  className={inputClass}
                  style={inputStyle}
                  placeholder="••••••••"
                  onFocus={(e) => e.target.style.borderColor = 'rgba(14,165,233,0.5)'}
                  onBlur={(e) => e.target.style.borderColor = 'rgba(26,51,84,0.8)'}
                />
              </div>
              <div>
                <button
                  className="flex items-center gap-2 rounded-lg px-5 py-2 text-xs font-bold text-white transition-all hover:opacity-90"
                  style={{ background: 'linear-gradient(135deg, #F59E0B, #D97706)', boxShadow: '0 4px 16px rgba(245,158,11,0.2)' }}
                >
                  <Lock className="h-3.5 w-3.5" /> Changer le mot de passe
                </button>
              </div>
            </div>
          </div>

          <div className="rounded-xl px-5 py-4" style={{ border: '1px solid rgba(26,51,84,0.8)', backgroundColor: '#0E1825' }}>
            <div className="flex items-center justify-between">
              <div className="flex items-center gap-3">
                <div
                  className="flex h-9 w-9 items-center justify-center rounded-xl"
                  style={{ backgroundColor: 'rgba(99,102,241,0.1)', border: '1px solid rgba(99,102,241,0.2)' }}
                >
                  <Bell className="h-4 w-4 text-violet-400" />
                </div>
                <div>
                  <p className="text-sm font-bold text-white">Authentification à deux facteurs</p>
                  <p className="text-[10px] text-slate-500 mt-0.5">Protégez votre compte avec la 2FA</p>
                </div>
              </div>
              <span
                className="text-[10px] font-semibold text-slate-500 px-3 py-1 rounded-full"
                style={{ border: '1px solid rgba(26,51,84,0.8)', backgroundColor: '#131F2E' }}
              >
                Bientôt
              </span>
            </div>
          </div>

          {/* Session info */}
          <div className="rounded-xl px-5 py-4" style={{ border: '1px solid rgba(26,51,84,0.8)', backgroundColor: '#0E1825' }}>
            <p className="text-xs font-bold text-white mb-3">Session active</p>
            <div className="space-y-2">
              {[
                { label: 'Statut', value: 'Connecté', color: 'text-green-400' },
                { label: 'Rôle', value: user?.role || 'USER', color: 'text-primary' },
                { label: 'ID utilisateur', value: user?.id ? String(user.id).slice(0, 8) + '...' : '—', color: 'text-slate-400' },
              ].map(({ label, value, color }) => (
                <div key={label} className="flex items-center justify-between">
                  <span className="text-xs text-slate-500">{label}</span>
                  <span className={`text-xs font-semibold font-mono ${color}`}>{value}</span>
                </div>
              ))}
            </div>
          </div>
        </div>
      )}
    </div>
  )
}