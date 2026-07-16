import { useState } from 'react'
import { useLocation, useNavigate } from 'react-router-dom'
import { useAuthStore } from '../store/authStore'
import { ChevronDown, User, LogOut, Settings, Menu, Zap } from 'lucide-react'

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
    <header
      className="fixed left-0 right-0 top-0 z-50 flex h-16 items-center px-4 lg:px-6"
      style={{
        borderBottom: '1px solid rgba(26,51,84,0.8)',
        backgroundColor: 'rgba(8,13,20,0.92)',
        backdropFilter: 'blur(12px)',
        WebkitBackdropFilter: 'blur(12px)',
      }}
    >
      {/* Left: hamburger + logo + separator + page title */}
      <div className="flex items-center gap-3 flex-1 min-w-0">
        <button
          onClick={onMenuToggle}
          className="flex h-8 w-8 items-center justify-center rounded-lg text-slate-400 hover:bg-white/5 hover:text-white transition-colors lg:hidden shrink-0"
        >
          <Menu className="h-4 w-4" />
        </button>

        {/* Logo */}
        <div
          className="flex items-center gap-2 cursor-pointer shrink-0"
          onClick={() => navigate('/dashboard')}
        >
          <div
            className="flex h-7 w-7 items-center justify-center rounded-lg"
            style={{ background: 'linear-gradient(135deg, #0EA5E9, #38BDF8)' }}
          >
            <Zap className="h-3.5 w-3.5 text-white" />
          </div>
          <span className="text-sm font-bold text-white hidden sm:block">
            Safozi<span className="text-primary"> Cloud</span>
          </span>
        </div>

        {/* Separator */}
        <div className="hidden lg:block h-4 w-px" style={{ backgroundColor: '#1A3354' }} />

        {/* Breadcrumb */}
        <div className="hidden lg:flex items-center gap-1.5 min-w-0">
          <span className="text-xs text-slate-500">Pages</span>
          <span className="text-xs text-slate-600">/</span>
          <span className="text-xs font-semibold text-slate-300 truncate">{title}</span>
        </div>
      </div>

      {/* Right: status + user */}
      <div className="flex items-center gap-2 shrink-0">
        {/* System status pill */}
        <div
          className="hidden sm:flex items-center gap-1.5 rounded-full px-3 py-1 text-xs font-medium text-green-400"
          style={{ border: '1px solid rgba(34,197,94,0.2)', backgroundColor: 'rgba(34,197,94,0.05)' }}
        >
          <span className="h-1.5 w-1.5 rounded-full bg-green-400 animate-pulse" />
          Tous les systèmes opérationnels
        </div>

        {/* User dropdown */}
        <div className="relative">
          <button
            onClick={() => setOpen((o) => !o)}
            className="flex items-center gap-2 rounded-lg px-2.5 py-1.5 text-sm text-slate-300 hover:bg-white/5 transition-colors"
          >
            <div
              className="flex h-7 w-7 items-center justify-center rounded-full text-xs font-bold text-white shrink-0"
              style={{ background: 'linear-gradient(135deg, #0EA5E9, #38BDF8)' }}
            >
              {initials || <User className="h-3.5 w-3.5" />}
            </div>
            <span className="hidden md:block font-medium text-xs max-w-28 truncate">{displayName}</span>
            <ChevronDown className={`h-3.5 w-3.5 text-slate-500 transition-transform ${open ? 'rotate-180' : ''}`} />
          </button>

          {open && (
            <>
              <div className="fixed inset-0 z-10" onClick={() => setOpen(false)} />
              <div
                className="absolute right-0 top-full z-20 mt-1.5 w-56 rounded-xl py-1 shadow-2xl"
                style={{ border: '1px solid #1A3354', backgroundColor: '#0E1825' }}
              >
                <div className="px-4 py-3" style={{ borderBottom: '1px solid #1A3354' }}>
                  <p className="text-xs font-bold text-white truncate">{displayName}</p>
                  <p className="text-[11px] text-slate-500 truncate mt-0.5">{user?.email}</p>
                  <span
                    className="mt-1.5 inline-block rounded px-1.5 py-0.5 text-[10px] font-bold text-primary"
                    style={{ backgroundColor: 'rgba(14,165,233,0.12)' }}
                  >
                    {user?.role || 'USER'}
                  </span>
                </div>
                <button
                  onClick={() => { setOpen(false); navigate('/settings') }}
                  className="flex w-full items-center gap-2.5 px-4 py-2.5 text-xs text-slate-400 hover:bg-white/5 hover:text-white transition-colors"
                >
                  <Settings className="h-3.5 w-3.5" /> Paramètres du compte
                </button>
                <div style={{ borderTop: '1px solid #1A3354' }}>
                  <button
                    onClick={() => { setOpen(false); logout(); navigate('/') }}
                    className="flex w-full items-center gap-2.5 px-4 py-2.5 text-xs text-red-400 hover:bg-red-500/10 hover:text-red-300 transition-colors"
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