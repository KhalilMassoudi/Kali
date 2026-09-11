import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { Eye, EyeOff, ArrowRight, Server, Shield, MessageSquare } from 'lucide-react'
import { useAuthStore } from '../store/authStore'
import safoziLogo from '../assets/safoziLogo.png'
import { Field, Input } from '../components/ui/Input'
import Button from '../components/ui/Button'

const FEATURES = [
  { icon: Server,        title: 'Compute',   text: 'Déployez des VMs en quelques secondes' },
  { icon: Shield,        title: 'Sécurité',  text: 'Infrastructure isolée et protégée' },
  { icon: MessageSquare, title: 'IA intégrée', text: 'Gérez votre cloud en langage naturel' },
]

export default function LoginPage() {
  const [email, setEmail]       = useState('')
  const [password, setPassword] = useState('')
  const [showPass, setShowPass] = useState(false)
  const [fieldErrors, setFieldErrors] = useState({})
  const [apiError, setApiError] = useState('')
  const { login, loading } = useAuthStore()
  const navigate = useNavigate()

  const validate = () => {
    const e = {}
    if (!email) e.email = 'Email requis'
    else if (!/\S+@\S+\.\S+/.test(email)) e.email = 'Format invalide'
    if (!password) e.password = 'Mot de passe requis'
    return e
  }

  const handleSubmit = async (ev) => {
    ev.preventDefault()
    setApiError('')
    const errors = validate()
    if (Object.keys(errors).length) { setFieldErrors(errors); return }
    setFieldErrors({})
    try {
      await login(email, password)
      navigate('/dashboard')
    } catch (err) {
      setApiError(err.response?.data?.message || 'Email ou mot de passe incorrect')
    }
  }

  return (
    <div className="flex min-h-screen bg-brand-bg">

      {/* ── Left branding panel ── */}
      <div className="relative hidden w-[460px] shrink-0 flex-col items-center justify-center overflow-hidden border-r border-brand-border/70 bg-[linear-gradient(180deg,#09172A_0%,#071020_100%)] px-12 lg:flex">
        <div
          className="pointer-events-none absolute inset-0 opacity-40"
          style={{ backgroundImage: 'radial-gradient(circle at 1px 1px, rgba(30,42,58,0.6) 1px, transparent 0)', backgroundSize: '28px 28px' }}
        />
        <div className="pointer-events-none absolute -top-[120px] left-1/2 h-[300px] w-[400px] -translate-x-1/2 rounded-full bg-[radial-gradient(circle,rgba(46,158,224,0.12),transparent)]" />

        <div className="relative z-[1] flex w-full max-w-[340px] flex-col gap-12">
          {/* Logo */}
          <div className="inline-block self-start border border-white/20 bg-white/[0.97] px-[22px] py-[10px]">
            <img src={safoziLogo} alt="SAFOZi" className="block h-9 object-contain" />
          </div>

          {/* Tagline */}
          <div>
            <div className="mb-6 inline-flex items-center gap-2 border border-primary/30 bg-primary/[0.06] px-3 py-1">
              <span className="block h-1.5 w-1.5 bg-primary" />
              <span className="text-2xs font-bold uppercase tracking-[0.12em] text-primary">Plateforme Cloud</span>
            </div>

            <h2 className="mb-5 text-4xl font-extrabold leading-[1.1] text-white">
              Gérez votre<br />infrastructure<br />
              <span className="text-accent">avec l'IA</span>
            </h2>

            <p className="text-sm leading-relaxed text-slate-500">
              Déployez, surveillez et optimisez vos serveurs cloud en langage naturel.
            </p>
          </div>

          {/* Feature list */}
          <div className="flex flex-col gap-6">
            {FEATURES.map(({ icon: Icon, title, text }) => (
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
            <span className="block h-[7px] w-[7px] bg-good animate-pulse" />
            <span className="text-xs font-semibold text-good">Tous les systèmes opérationnels</span>
          </div>
        </div>
      </div>

      {/* ── Right form panel ── */}
      <div className="flex flex-1 flex-col items-center justify-center px-10 py-12">
        {/* Mobile logo */}
        <div className="mb-10 inline-block border border-white/15 bg-white/[0.97] px-[22px] py-[10px] lg:hidden">
          <img src={safoziLogo} alt="SAFOZi" className="block h-8 object-contain" />
        </div>

        <div className="w-full max-w-[480px]">
          {/* Heading */}
          <div className="mb-10">
            <div className="mb-5 inline-block border border-brand-border px-2.5 py-[3px] text-2xs font-bold uppercase tracking-[0.12em] text-slate-600">
              Authentification
            </div>
            <h1 className="mb-2 text-3xl font-extrabold leading-tight text-white">Connexion</h1>
            <p className="text-[0.9375rem] text-slate-600">Accédez à votre espace cloud Safozi</p>
          </div>

          {/* API error */}
          {apiError && (
            <div className="mb-6 flex items-center gap-3 border border-crit/40 bg-crit/[0.06] px-4 py-3">
              <span className="block h-2 w-2 shrink-0 bg-crit" />
              <p className="text-sm font-medium text-crit">{apiError}</p>
            </div>
          )}

          {/* Form */}
          <form onSubmit={handleSubmit}>
            <Field label="Adresse email" error={fieldErrors.email} className="mb-6">
              <Input
                type="email"
                placeholder="vous@exemple.com"
                value={email}
                error={!!fieldErrors.email}
                onChange={(e) => { setEmail(e.target.value); setFieldErrors((f) => ({ ...f, email: '' })) }}
                autoComplete="email"
              />
            </Field>

            <Field label="Mot de passe" error={fieldErrors.password} className="mb-8">
              <div className="relative">
                <Input
                  type={showPass ? 'text' : 'password'}
                  className="pr-14"
                  placeholder="••••••••"
                  value={password}
                  error={!!fieldErrors.password}
                  onChange={(e) => { setPassword(e.target.value); setFieldErrors((f) => ({ ...f, password: '' })) }}
                  autoComplete="current-password"
                />
                <button
                  type="button"
                  onClick={() => setShowPass((v) => !v)}
                  className="absolute right-4 top-1/2 flex -translate-y-1/2 items-center text-slate-600 transition-colors hover:text-slate-300"
                >
                  {showPass ? <EyeOff className="h-[18px] w-[18px]" /> : <Eye className="h-[18px] w-[18px]" />}
                </button>
              </div>
            </Field>

            {/* Submit */}
            <Button type="submit" variant="primary" size="lg" loading={loading} className="w-full">
              {loading ? 'Connexion en cours...' : <>Se connecter <ArrowRight className="h-[18px] w-[18px]" /></>}
            </Button>
          </form>

          {/* Footer link */}
          <p className="mt-8 text-center text-sm text-slate-600">
            Pas encore de compte ?{' '}
            <Link to="/register" className="font-bold text-primary transition-colors hover:text-accent">
              Créer un compte gratuitement
            </Link>
          </p>
        </div>
      </div>
    </div>
  )
}
