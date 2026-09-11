import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { Eye, EyeOff, ArrowRight, Check, Server, Shield, MessageSquare } from 'lucide-react'
import { useAuthStore } from '../store/authStore'
import safoziLogo from '../assets/safoziLogo.png'
import { Field, Input } from '../components/ui/Input'
import Button from '../components/ui/Button'

const PERKS = [
  { icon: Server,        title: 'Compute',    text: 'VMs déployées en secondes' },
  { icon: Shield,        title: 'Sécurité',   text: 'Isolation complète de votre infra' },
  { icon: MessageSquare, title: 'IA incluse',  text: 'Assistant cloud sans surcoût' },
]

const STRENGTH = [
  null,
  { label: 'Faible',  color: 'bg-crit',    text: 'text-crit' },
  { label: 'Correct', color: 'bg-warn',    text: 'text-warn' },
  { label: 'Fort',    color: 'bg-good',    text: 'text-good' },
]

export default function RegisterPage() {
  const [form, setForm] = useState({ firstName: '', lastName: '', email: '', password: '', confirm: '' })
  const [showPass, setShowPass]   = useState(false)
  const [fieldErrors, setFieldErrors] = useState({})
  const [apiError, setApiError]   = useState('')
  const { register, loading } = useAuthStore()
  const navigate = useNavigate()

  const set = (k, v) => {
    setForm((f) => ({ ...f, [k]: v }))
    setFieldErrors((e) => ({ ...e, [k]: '' }))
  }

  const validate = () => {
    const e = {}
    if (!form.firstName.trim()) e.firstName = 'Prénom requis'
    if (!form.email) e.email = 'Email requis'
    else if (!/\S+@\S+\.\S+/.test(form.email)) e.email = 'Format invalide'
    if (!form.password) e.password = 'Mot de passe requis'
    else if (form.password.length < 6) e.password = 'Minimum 6 caractères'
    if (form.password && form.confirm !== form.password) e.confirm = 'Les mots de passe ne correspondent pas'
    return e
  }

  const handleSubmit = async (ev) => {
    ev.preventDefault()
    setApiError('')
    const errors = validate()
    if (Object.keys(errors).length) { setFieldErrors(errors); return }
    setFieldErrors({})
    try {
      await register(form.email, form.password, form.firstName, form.lastName)
      navigate('/dashboard')
    } catch (err) {
      setApiError(err.response?.data?.message || 'Erreur lors de la création du compte')
    }
  }

  const passwordStrength = form.password.length === 0 ? 0
    : form.password.length < 6  ? 1
    : form.password.length < 10 ? 2 : 3
  const strength = STRENGTH[passwordStrength]
  const confirmOk = form.confirm && form.confirm === form.password

  return (
    <div className="flex min-h-screen bg-brand-bg">

      {/* ── Left branding panel ── */}
      <div className="relative hidden w-[440px] shrink-0 flex-col items-center justify-center overflow-hidden border-r border-brand-border/70 bg-[linear-gradient(180deg,#09172A_0%,#071020_100%)] px-12 lg:flex">
        <div
          className="pointer-events-none absolute inset-0 opacity-40"
          style={{ backgroundImage: 'radial-gradient(circle at 1px 1px, rgba(30,42,58,0.6) 1px, transparent 0)', backgroundSize: '28px 28px' }}
        />
        <div className="pointer-events-none absolute -top-[120px] left-1/2 h-[300px] w-[400px] -translate-x-1/2 bg-[radial-gradient(circle,rgba(46,158,224,0.12),transparent)]" />

        <div className="relative z-[1] flex w-full max-w-[320px] flex-col gap-11">
          {/* Logo */}
          <div className="inline-block self-start border border-white/20 bg-white/[0.97] px-[22px] py-[10px]">
            <img src={safoziLogo} alt="SAFOZi" className="block h-9 object-contain" />
          </div>

          {/* Tagline */}
          <div>
            <div className="mb-6 inline-flex items-center gap-2 border border-primary/30 bg-primary/[0.06] px-3 py-1">
              <span className="block h-1.5 w-1.5 bg-primary" />
              <span className="text-2xs font-bold uppercase tracking-[0.12em] text-primary">Inscription gratuite</span>
            </div>

            <h2 className="mb-5 text-[2.25rem] font-extrabold leading-[1.1] text-white">
              Votre cloud<br />opérationnel en<br />
              <span className="text-accent">5 minutes</span>
            </h2>

            <p className="text-sm leading-relaxed text-slate-500">
              Créez votre compte et déployez votre première VM immédiatement.
              Aucune carte bancaire requise.
            </p>
          </div>

          {/* Feature list */}
          <div className="flex flex-col gap-6">
            {PERKS.map(({ icon: Icon, title, text }) => (
              <div key={title} className="flex items-start gap-4">
                <div className="flex h-10 w-10 shrink-0 items-center justify-center border border-primary/25 bg-primary/[0.08]">
                  <Icon className="h-[18px] w-[18px] text-primary" />
                </div>
                <div>
                  <p className="mb-0.5 text-[0.8125rem] font-bold text-slate-200">{title}</p>
                  <p className="text-xs leading-relaxed text-slate-600">{text}</p>
                </div>
              </div>
            ))}
          </div>

          {/* Status */}
          <div className="inline-flex items-center gap-2 self-start border border-good/25 bg-good/[0.05] px-3.5 py-[7px]">
            <span className="block h-[7px] w-[7px] bg-good" />
            <span className="text-xs font-semibold text-good">Inscription ouverte · Aucune CB requise</span>
          </div>
        </div>
      </div>

      {/* ── Right form panel ── */}
      <div className="flex flex-1 flex-col items-center justify-center overflow-y-auto px-10 py-10">
        {/* Mobile logo */}
        <div className="mb-10 inline-block border border-white/15 bg-white/[0.97] px-[22px] py-[10px] lg:hidden">
          <img src={safoziLogo} alt="SAFOZi" className="block h-8 object-contain" />
        </div>

        <div className="w-full max-w-[520px]">
          {/* Heading */}
          <div className="mb-10">
            <div className="mb-5 inline-block border border-brand-border px-2.5 py-[3px] text-2xs font-bold uppercase tracking-[0.12em] text-slate-600">
              Nouveau compte
            </div>
            <h1 className="mb-2 text-3xl font-extrabold leading-tight text-white">Créer un compte</h1>
            <p className="text-[0.9375rem] text-slate-600">Rejoignez Safozi Cloud — c'est gratuit</p>
          </div>

          {/* API error */}
          {apiError && (
            <div className="mb-6 flex items-center gap-3 border border-crit/40 bg-crit/[0.06] px-4 py-3">
              <span className="block h-2 w-2 shrink-0 bg-crit" />
              <p className="text-sm font-medium text-crit">{apiError}</p>
            </div>
          )}

          <form onSubmit={handleSubmit}>
            {/* Name row */}
            <div className="mb-6 grid grid-cols-1 gap-4 sm:grid-cols-2">
              <Field label="Prénom" error={fieldErrors.firstName}>
                <Input
                  placeholder="Jean"
                  value={form.firstName}
                  error={!!fieldErrors.firstName}
                  onChange={(e) => set('firstName', e.target.value)}
                />
              </Field>
              <Field label="Nom">
                <Input
                  placeholder="Dupont"
                  value={form.lastName}
                  onChange={(e) => set('lastName', e.target.value)}
                />
              </Field>
            </div>

            {/* Email */}
            <Field label="Adresse email" error={fieldErrors.email} className="mb-6">
              <Input
                type="email"
                placeholder="vous@exemple.com"
                value={form.email}
                error={!!fieldErrors.email}
                onChange={(e) => set('email', e.target.value)}
                autoComplete="email"
              />
            </Field>

            {/* Password */}
            <Field label="Mot de passe" error={fieldErrors.password} className="mb-6">
              <div className="relative">
                <Input
                  type={showPass ? 'text' : 'password'}
                  className="pr-14"
                  placeholder="Min. 6 caractères"
                  value={form.password}
                  error={!!fieldErrors.password}
                  onChange={(e) => set('password', e.target.value)}
                  autoComplete="new-password"
                />
                <button
                  type="button"
                  onClick={() => setShowPass((v) => !v)}
                  className="absolute right-4 top-1/2 flex -translate-y-1/2 items-center text-slate-600 transition-colors hover:text-slate-300"
                >
                  {showPass ? <EyeOff className="h-[18px] w-[18px]" /> : <Eye className="h-[18px] w-[18px]" />}
                </button>
              </div>
              {!fieldErrors.password && form.password && (
                <div className="mt-2 flex items-center gap-2.5">
                  <div className="flex flex-1 gap-[3px]">
                    {[1, 2, 3].map((lvl) => (
                      <div key={lvl} className={`h-[3px] flex-1 transition-colors ${lvl <= passwordStrength ? strength.color : 'bg-brand-border'}`} />
                    ))}
                  </div>
                  <span className={`min-w-[40px] text-xs font-bold ${strength.text}`}>{strength.label}</span>
                </div>
              )}
            </Field>

            {/* Confirm */}
            <Field label="Confirmer le mot de passe" error={fieldErrors.confirm} className="mb-8">
              <div className="relative">
                <Input
                  type="password"
                  className="pr-14"
                  placeholder="••••••••"
                  value={form.confirm}
                  onChange={(e) => set('confirm', e.target.value)}
                />
                {confirmOk && (
                  <div className="absolute right-4 top-1/2 flex h-[22px] w-[22px] -translate-y-1/2 items-center justify-center border border-good/40 bg-good/15">
                    <Check className="h-[13px] w-[13px] text-good" />
                  </div>
                )}
              </div>
            </Field>

            {/* Submit */}
            <Button type="submit" variant="primary" size="lg" loading={loading} className="w-full">
              {loading ? 'Création du compte...' : <>Créer mon compte <ArrowRight className="h-[18px] w-[18px]" /></>}
            </Button>
          </form>

          <p className="mt-8 text-center text-sm text-slate-600">
            Déjà un compte ?{' '}
            <Link to="/" className="font-bold text-primary transition-colors hover:text-accent">
              Se connecter
            </Link>
          </p>
        </div>
      </div>
    </div>
  )
}
