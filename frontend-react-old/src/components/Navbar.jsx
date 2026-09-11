import { useState } from 'react'
import { useLocation, useNavigate } from 'react-router-dom'
import { useAuthStore } from '../store/authStore'
import { ChevronDown, User, LogOut, Settings, Menu } from 'lucide-react'
import safoziLogo from '../assets/safoziLogo.png'
import Button from './ui/Button'

const pageTitles = {
  '/dashboard': 'Dashboard',
  '/vms':       'Machines Virtuelles',
  '/chat':      'Assistant IA',
  '/settings':  'Paramètres',
}

export default function Navbar({ onMenuToggle }) {
  const [open, setOpen] = useState(false)
  const user     = useAuthStore((s) => s.user)
  const logout   = useAuthStore((s) => s.logout)
  const location = useLocation()
  const navigate = useNavigate()

  const title       = pageTitles[location.pathname] || 'Safozi Cloud'
  const displayName = user
    ? `${user.firstName || ''} ${user.lastName || ''}`.trim() || user.email
    : 'Utilisateur'
  const initials    = displayName.split(' ').map((w) => w[0]).join('').toUpperCase().slice(0, 2)

  return (
    <header className="fixed left-0 right-0 top-0 z-50 flex h-16 items-center border-b border-brand-border/80 bg-brand-bg/[0.92] px-4 backdrop-blur-md lg:px-6">
      {/* Left: hamburger + logo + separator + page title */}
      <div className="flex min-w-0 flex-1 items-center gap-3">
        <Button variant="ghost" size="iconMd" onClick={onMenuToggle} className="shrink-0 lg:hidden">
          <Menu className="h-4 w-4" />
        </Button>

        {/* Logo */}
        <div className="shrink-0 cursor-pointer bg-white/95 px-3 py-1.5" onClick={() => navigate('/dashboard')}>
          <img src={safoziLogo} alt="SAFOZi" className="h-7 object-contain" />
        </div>

        {/* Separator */}
        <div className="hidden h-4 w-px bg-brand-border lg:block" />

        {/* Breadcrumb */}
        <div className="hidden min-w-0 items-center gap-1.5 lg:flex">
          <span className="text-xs text-slate-500">Pages</span>
          <span className="text-xs text-slate-600">/</span>
          <span className="truncate text-xs font-semibold text-slate-300">{title}</span>
        </div>
      </div>

      {/* Right: status + user */}
      <div className="flex shrink-0 items-center gap-2">
        {/* System status pill */}
        <div className="hidden items-center gap-1.5 border border-good/20 bg-good/[0.05] px-3 py-1 text-xs font-medium text-good sm:flex">
          <span className="h-1.5 w-1.5 rounded-full bg-good animate-pulse" />
          Tous les systèmes opérationnels
        </div>

        {/* User dropdown */}
        <div className="relative">
          <button
            onClick={() => setOpen((o) => !o)}
            className="flex items-center gap-2 px-2.5 py-1.5 text-sm text-slate-300 transition-colors hover:bg-white/5"
          >
            <div className="flex h-7 w-7 shrink-0 items-center justify-center rounded-full bg-[linear-gradient(135deg,#2E9EE0,#5CC8F7)] text-xs font-bold text-white">
              {initials || <User className="h-3.5 w-3.5" />}
            </div>
            <span className="hidden max-w-28 truncate text-xs font-medium md:block">{displayName}</span>
            <ChevronDown className={`h-3.5 w-3.5 text-slate-500 transition-transform ${open ? 'rotate-180' : ''}`} />
          </button>

          {open && (
            <>
              <div className="fixed inset-0 z-10" onClick={() => setOpen(false)} />
              <div className="absolute right-0 top-full z-20 mt-1.5 w-56 border border-brand-border bg-brand-surface py-1 shadow-2xl">
                <div className="border-b border-brand-border px-4 py-3">
                  <p className="truncate text-xs font-bold text-white">{displayName}</p>
                  <p className="mt-0.5 truncate text-2xs text-slate-500">{user?.email}</p>
                  <span className="mt-1.5 inline-block bg-primary/[0.12] px-1.5 py-0.5 text-[10px] font-bold text-primary">
                    {user?.role || 'USER'}
                  </span>
                </div>
                <button
                  onClick={() => { setOpen(false); navigate('/settings') }}
                  className="flex w-full items-center gap-2.5 px-4 py-2.5 text-xs text-slate-400 transition-colors hover:bg-white/5 hover:text-white"
                >
                  <Settings className="h-3.5 w-3.5" /> Paramètres du compte
                </button>
                <div className="border-t border-brand-border">
                  <button
                    onClick={() => { setOpen(false); logout(); navigate('/') }}
                    className="flex w-full items-center gap-2.5 px-4 py-2.5 text-xs text-crit transition-colors hover:bg-crit/10"
                  >
                    <LogOut className="h-3.5 w-3.5" /> Déconnexion
                  </button>
                </div>
              </div>
            </>
          )}
        </div>
      </div>
    </header>
  )
}