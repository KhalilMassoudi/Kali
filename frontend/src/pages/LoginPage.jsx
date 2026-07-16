import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { Eye, EyeOff, Loader2, Cloud, ArrowRight } from 'lucide-react'
import { useAuthStore } from '../store/authStore'

export default function LoginPage() {
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [showPass, setShowPass] = useState(false)
  const [fieldErrors, setFieldErrors] = useState({})
  const [apiError, setApiError] = useState('')
  const { login, loading } = useAuthStore()
  const navigate = useNavigate()

  const validate = () => {
    const errors = {}
    if (!email) errors.email = 'Email requis'
    else if (!/\S+@\S+\.\S+/.test(email)) errors.email = 'Format invalide'
    if (!password) errors.password = 'Mot de passe requis'
    return errors
  }

  const handleSubmit = async (e) => {
    e.preventDefault()
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

  const inputClass = (field) =>
    `w-full rounded-xl px-4 py-3.5 text-sm text-white placeholder-gray-500 transition-all focus:outline-none ${
      fieldErrors[field]
        ? 'border-2 border-red-500 bg-red-500/5 focus:ring-2 focus:ring-red-500/30'
        : 'border border-brand-border bg-brand-surface2 focus:border-primary focus:ring-2 focus:ring-primary/20'
    }`

  return (
    <div className="flex min-h-screen items-center justify-center bg-brand-bg p-4">
      {/* Background glow */}
      <div className="pointer-events-none fixed inset-0 overflow-hidden">
        <div className="absolute -top-40 left-1/2 h-80 w-80 -translate-x-1/2 rounded-full opacity-10"
          style={{ background: 'radial-gradient(circle, #00A3FF, transparent)' }} />
      </div>

      <div className="w-full max-w-md relative">
        {/* Logo */}
        <div className="mb-10 flex flex-col items-center gap-4">
          <div
            className="flex h-16 w-16 items-center justify-center rounded-2xl shadow-2xl"
            style={{ background: 'linear-gradient(135deg, #00A3FF, #00D9FF)', boxShadow: '0 0 40px rgba(0,163,255,0.3)' }}
          >
            <Cloud className="h-8 w-8 text-white" />
          </div>
          <div className="text-center">
            <h1 className="text-3xl font-bold">
              <span className="text-white">Safozi</span>
              <span className="text-primary"> Cloud</span>
            </h1>
            <p className="mt-2 text-sm text-gray-400">Connectez-vous à votre espace cloud</p>
          </div>
        </div>

        {/* Card */}
        <div
          className="rounded-2xl p-8 shadow-2xl"
          style={{ border: '1px solid #1E3A5F', backgroundColor: '#111C2D' }}
        >
          <h2 className="mb-6 text-xl font-bold text-white">Connexion</h2>

          {apiError && (
            <div className="mb-6 flex items-center gap-3 rounded-xl border border-red-500/30 bg-red-500/10 px-4 py-3.5">
              <div className="h-2 w-2 rounded-full bg-red-500 shrink-0" />
              <p className="text-sm font-medium text-red-300">{apiError}</p>
            </div>
          )}

          <form onSubmit={handleSubmit} className="space-y-5">
            <div>
              <label className="mb-2 block text-sm font-semibold text-gray-300">Adresse email</label>
              <input
                type="email"
                className={inputClass('email')}
                placeholder="vous@exemple.com"
                value={email}
                onChange={(e) => { setEmail(e.target.value); setFieldErrors((f) => ({ ...f, email: '' })) }}
                autoComplete="email"
              />
              {fieldErrors.email && <p className="mt-1.5 text-xs text-red-400">{fieldErrors.email}</p>}
            </div>

            <div>
              <label className="mb-2 block text-sm font-semibold text-gray-300">Mot de passe</label>
              <div className="relative">
                <input
                  type={showPass ? 'text' : 'password'}
                  className={`${inputClass('password')} pr-12`}
                  placeholder="••••••••"
                  value={password}
                  onChange={(e) => { setPassword(e.target.value); setFieldErrors((f) => ({ ...f, password: '' })) }}
                  autoComplete="current-password"
                />
                <button
                  type="button"
                  onClick={() => setShowPass((v) => !v)}
                  className="absolute right-3.5 top-1/2 -translate-y-1/2 text-gray-500 hover:text-gray-300 transition-colors"
                >
                  {showPass ? <EyeOff className="h-4 w-4" /> : <Eye className="h-4 w-4" />}
                </button>
              </div>
              {fieldErrors.password && <p className="mt-1.5 text-xs text-red-400">{fieldErrors.password}</p>}
            </div>

            <button
              type="submit"
              disabled={loading}
              className="flex w-full items-center justify-center gap-2 rounded-xl py-3.5 text-sm font-bold text-white transition-all disabled:opacity-60 mt-2"
              style={{
                background: loading ? '#0078C8' : 'linear-gradient(135deg, #00A3FF, #00D9FF)',
                boxShadow: '0 4px 20px rgba(0, 163, 255, 0.3)',
              }}
            >
              {loading ? (
                <><Loader2 className="h-4 w-4 animate-spin" /> Connexion en cours...</>
              ) : (
                <>Se connecter <ArrowRight className="h-4 w-4" /></>
              )}
            </button>
          </form>

          <p className="mt-6 text-center text-sm text-gray-500">
            Pas encore de compte ?{' '}
            <Link to="/register" className="font-semibold text-primary hover:text-accent transition-colors">
              Créer un compte
            </Link>
          </p>
        </div>
      </div>
    </div>
  )
}