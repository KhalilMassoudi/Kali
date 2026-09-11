import { useState } from 'react'
import { User, Mail, Shield, Save, Loader2, CheckCircle, Key, Bell, Lock } from 'lucide-react'
import { useAuthStore } from '../store/authStore'
import { Card } from '../components/ui/Card'
import Button from '../components/ui/Button'
import { Field, Input } from '../components/ui/Input'

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

  const displayName = user ? `${user.firstName || ''} ${user.lastName || ''}`.trim() : ''
  const initials = displayName.split(' ').map((w) => w[0]).join('').toUpperCase().slice(0, 2)

  return (
    <div className="w-full px-6 py-6">
      {/* Page header */}
      <div className="mb-5">
        <h2 className="text-xl font-bold text-white">Paramètres</h2>
        <p className="mt-0.5 text-xs text-slate-500">Gérez votre compte et vos préférences</p>
      </div>

      {/* Tabs */}
      <div className="inline-flex gap-1 mb-5 p-1 border border-brand-border bg-brand-surface">
        {TABS.map(({ key, label, icon: Icon }) => (
          <button
            key={key}
            onClick={() => setTab(key)}
            className={`flex items-center gap-2 px-4 py-2 text-xs font-bold transition-all ${
              tab === key
                ? 'text-white bg-[linear-gradient(135deg,#2E9EE0,#0284C7)] shadow-[0_2px_8px_rgba(46,158,224,0.2)]'
                : 'text-slate-500 hover:text-slate-300'
            }`}
          >
            <Icon className="h-3.5 w-3.5" /> {label}
          </button>
        ))}
      </div>

      {/* Profile tab */}
      {tab === 'profile' && (
        <Card className="overflow-hidden">
          {/* Avatar section */}
          <div className="p-6 border-b border-brand-border">
            <div className="flex items-center gap-4">
              <div className="flex h-16 w-16 shrink-0 items-center justify-center text-2xl font-bold text-white bg-[linear-gradient(135deg,#2E9EE0,#5CC8F7)] shadow-[0_0_24px_rgba(46,158,224,0.2)]">
                {initials || '?'}
              </div>
              <div>
                <h3 className="text-base font-bold text-white">{displayName || 'Utilisateur'}</h3>
                <p className="text-xs text-slate-500 mt-0.5">{user?.email}</p>
                <div className="mt-2 flex gap-2">
                  <span className="inline-flex items-center px-2.5 py-0.5 text-[10px] font-bold text-primary border border-primary/30 bg-primary/[0.08]">
                    {user?.role || 'USER'}
                  </span>
                  <span className="inline-flex items-center gap-1 px-2.5 py-0.5 text-[10px] font-bold text-good border border-good/30 bg-good/[0.08]">
                    <span className="h-1 w-1 rounded-full bg-good animate-pulse" /> Actif
                  </span>
                </div>
              </div>
            </div>
          </div>

          <form onSubmit={handleSave} className="p-6 space-y-5">
            <div className="grid grid-cols-1 gap-3 sm:grid-cols-2">
              <Field label="Prénom">
                <Input defaultValue={user?.firstName || ''} placeholder="Jean" />
              </Field>
              <Field label="Nom">
                <Input defaultValue={user?.lastName || ''} placeholder="Dupont" />
              </Field>
            </div>

            <Field
              label="Adresse email"
              icon={Mail}
              hint="L'adresse email ne peut pas être modifiée"
            >
              <Input type="email" defaultValue={user?.email || ''} disabled className="border-brand-border/50 bg-brand-surface opacity-60" />
            </Field>

            <div className="flex items-center justify-between pt-1">
              <p className="text-[10px] text-slate-700 font-mono">
                ID: {user?.id || '—'}
              </p>
              <Button type="submit" variant="primary" disabled={loading}>
                {loading ? (
                  <><Loader2 className="h-3.5 w-3.5 animate-spin" /> Enregistrement...</>
                ) : saved ? (
                  <><CheckCircle className="h-3.5 w-3.5" /> Enregistré !</>
                ) : (
                  <><Save className="h-3.5 w-3.5" /> Enregistrer</>
                )}
              </Button>
            </div>
          </form>
        </Card>
      )}

      {/* Security tab */}
      {tab === 'security' && (
        <div className="space-y-4">
          <Card className="overflow-hidden">
            <div className="flex items-center gap-3 p-6 border-b border-brand-border">
              <div className="flex h-9 w-9 items-center justify-center bg-warn/10 border border-warn/20">
                <Key className="h-4 w-4 text-warn" />
              </div>
              <div>
                <h3 className="text-sm font-bold text-white">Changer le mot de passe</h3>
                <p className="text-[10px] text-slate-500 mt-0.5">Mettez à jour votre mot de passe régulièrement</p>
              </div>
            </div>
            <div className="p-6 space-y-5">
              <Field label="Mot de passe actuel">
                <Input type="password" placeholder="••••••••" />
              </Field>
              <Field label="Nouveau mot de passe">
                <Input type="password" placeholder="Min. 6 caractères" />
              </Field>
              <Field label="Confirmer le mot de passe">
                <Input type="password" placeholder="••••••••" />
              </Field>
              <div>
                <Button
                  variant="primary"
                  className="bg-[linear-gradient(135deg,#F0B429,#D97706)] shadow-[0_4px_16px_rgba(240,180,41,0.2)]"
                >
                  <Lock className="h-3.5 w-3.5" /> Changer le mot de passe
                </Button>
              </div>
            </div>
          </Card>

          <Card className="p-6">
            <div className="flex items-center justify-between">
              <div className="flex items-center gap-3">
                <div className="flex h-9 w-9 items-center justify-center bg-accent/10 border border-accent/20">
                  <Bell className="h-4 w-4 text-accent" />
                </div>
                <div>
                  <p className="text-sm font-bold text-white">Authentification à deux facteurs</p>
                  <p className="text-[10px] text-slate-500 mt-0.5">Protégez votre compte avec la 2FA</p>
                </div>
              </div>
              <span className="text-[10px] font-semibold text-slate-500 px-3 py-1 border border-brand-border bg-brand-surface2">
                Bientôt
              </span>
            </div>
          </Card>

          {/* Session info */}
          <Card className="p-6">
            <p className="text-xs font-bold text-white mb-3">Session active</p>
            <div className="space-y-2">
              {[
                { label: 'Statut', value: 'Connecté', color: 'text-good' },
                { label: 'Rôle', value: user?.role || 'USER', color: 'text-primary' },
                { label: 'ID utilisateur', value: user?.id ? String(user.id).slice(0, 8) + '...' : '—', color: 'text-slate-400' },
              ].map(({ label, value, color }) => (
                <div key={label} className="flex items-center justify-between">
                  <span className="text-xs text-slate-500">{label}</span>
                  <span className={`text-xs font-semibold font-mono ${color}`}>{value}</span>
                </div>
              ))}
            </div>
          </Card>
        </div>
      )}
    </div>
  )
}