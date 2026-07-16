import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { Eye, EyeOff, Loader2, Cloud, Check, ArrowRight } from 'lucide-react'
import { useAuthStore } from '../store/authStore'

export default function RegisterPage() {
  const [form, setForm] = useState({ firstName: '', lastName: '', email: '', password: '', confirm: '' })
  const [showPass, setShowPass] = useState(false)
  const [fieldErrors, setFieldErrors] = useState({})
  const [apiError, setApiError] = useState('')
  const { register, loading } = useAuthStore()
  const navigate = useNavigate()

  const set = (k, v) => {
    setForm((f) => ({ ...f, [k]: v }))
    setFieldErrors((e) => ({ ...e, [k]: '' }))
  }

  const validate = () => {
    const errors = {}
    if (!form.firstName.trim()) errors.firstName = 'Prénom requis'
    if (!form.email) errors.email = 'Email requis'
    else if (!/\S+@\S+\.\S+/.test(form.email)) errors.email = 'Format invalide'
    if (!form.password) errors.password = 'Mot de passe requis'
    else if (form.password.length < 6) errors.password = 'Minimum 6 caractères'
    if (form.password && form.confirm !== form.password) errors.confirm = 'Les mots de passe ne correspondent pas'
    return errors
  }

  const handleSubmit = async (e) => {
    e.preventDefault()
    setApiError('')
    const errors = validate()
    if (Object.keys(errors).length) { setFieldErrors(errors); return }
    setFieldErrors({})
    try {
      await register(form.email, form.password, form.firstName, form.lastName)
      navigate('/dashboard')
    } catch (err) {
      const msg = err.response?.data?.message || 'Erreur lors de la création du compte'
      setApiError(msg)
    }
  }

  const inputClass = (field) =>
    `w-full rounded-xl px-4 py-3.5 text-sm text-white placeholder-gray-500 transition-all focus:outline-none ${
      fieldErrors[field]
        ? 'border-2 border-red-500 bg-red-500/5 focus:ring-2 focus:ring-red-500/30'
        : 'border border-brand-border bg-brand-surface2 focus:border-primary focus:ring-2 focus:ring-primary/20'
    }`

  const passwordOk = form.password.length >= 6
  const confirmOk = form.confirm && form.confirm === form.password

  return (
    <div className="flex min-h-screen items-center justify-center bg-brand-bg p-4">
      <div className="pointer-events-none fixed inset-0 overflow-hidden">
        <div className="absolute -top-40 left-1/2 h-80 w-80 -translate-x-1/2 rounded-full opacity-10"
          style={{ background: 'radial-gradient(circle, #00A3FF, transparent)' }} />
      </div>

      <div className="w-full max-w-md relative">
        {/* Logo */}
        <div className="mb-8 flex flex-col items-center gap-4">
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
            <p className="mt-2 text-sm text-gray-400">Créez votre compte gratuitement</p>
          </div>
        </div>

        <div
          className="rounded-2xl p-8 shadow-2xl"
          style={{ border: '1px solid #1E3A5F', backgroundColor: '#111C2D' }}
        >
          <h2 className="mb-6 text-xl font-bold text-white">Créer un compte</h2>

          {apiError && (
            <div className="mb-6 flex items-center gap-3 rounded-xl border border-red-500/30 bg-red-500/10 px-4 py-3.5">
              <div className="h-2 w-2 rounded-full bg-red-500 shrink-0" />
              <p className="text-sm font-medium text-red-300">{apiError}</p>
            </div>
          )}

          <form onSubmit={handleSubmit} className="space-y-4">
            <div className="grid grid-cols-2 gap-3">
              <div>
                <label className="mb-2 block text-sm font-semibold text-gray-300">Prénom</label>
                <input className={inputClass('firstName')} placeholder="Jean" value={form.firstName} onChange={(e) => set('firstName', e.target.value)} />
                {fieldErrors.firstName && <p className="mt-1 text-xs text-red-400">{fieldErrors.firstName}</p>}
              </div>
              <div>
                <label className="mb-2 block text-sm font-semibold text-gray-300">Nom</label>
                <input className={inputClass('lastName')} placeholder="Dupont" value={form.lastName} onChange={(e) => set('lastName', e.target.value)} />
              </div>
            </div>

            <div>
              <label className="mb-2 block text-sm font-semibold text-gray-300">Adresse email</label>
              <input
                type="email"
                className={inputClass('email')}
                placeholder="vous@exemple.com"
                value={form.email}
                onChange={(e) => set('email', e.target.value)}
                autoComplete="email"
              />
              {fieldErrors.email && <p className="mt-1 text-xs text-red-400">{fieldErrors.email}</p>}
            </div>

            <div>
              <label className="mb-2 block text-sm font-semibold text-gray-300">Mot de passe</label>
              <div className="relative">
                <input
                  type={showPass ? 'text' : 'password'}
                  className={`${inputClass('password')} pr-12`}
                  placeholder="Min. 6 caractères"
                  value={form.password}
                  onChange={(e) => set('password', e.target.value)}
                  autoComplete="new-password"
                />
                <button type="button" onClick={() => setShowPass((v) => !v)}
                  className="absolute right-3.5 top-1/2 -translate-y-1/2 text-gray-500 hover:text-gray-300 transition-colors">
                  {showPass ? <EyeOff className="h-4 w-4" /> : <Eye className="h-4 w-4" />}
                </button>
              </div>
              {fieldErrors.password
                ? <p className="mt-1 text-xs text-red-400">{fieldErrors.password}</p>
                : form.password && (
                  <p className={`mt-1 flex items-center gap-1 text-xs ${passwordOk ? 'text-green-400' : 'text-gray-500'}`}>
                    {passwordOk && <Check className="h-3 w-3" />}
                    {passwordOk ? 'Mot de passe valide' : `${6 - form.password.length} caractère(s) manquant(s)`}
                  </p>
                )
              }
            </div>

            <div>
              <label className="mb-2 block text-sm font-semibold text-gray-300">Confirmer le mot de passe</label>
              <div className="relative">
                <input
                  type="password"
                  className={`${inputClass('confirm')} pr-10`}
                  placeholder="••••••••"
                  value={form.confirm}
                  onChange={(e) => set('confirm', e.target.value)}
                />
                {confirmOk && (
                  <Check className="absolute right-3.5 top-1/2 -translate-y-1/2 h-4 w-4 text-green-400" />
                )}
              </div>
              {fieldErrors.confirm && <p className="mt-1 text-xs text-red-400">{fieldErrors.confirm}</p>}
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
                <><Loader2 className="h-4 w-4 animate-spin" /> Création du compte...</>
              ) : (
                <>Créer mon compte <ArrowRight className="h-4 w-4" /></>
              )}
            </button>
          </form>

          <p className="mt-6 text-center text-sm text-gray-500">
            Déjà un compte ?{' '}
            <Link to="/" className="font-semibold text-primary hover:text-accent transition-colors">
              Se connecter
            </Link>
          </p>
        </div>
      </div>
    </div>
  )
}