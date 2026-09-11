import { NavLink, useNavigate } from 'react-router-dom'
import { LayoutDashboard, Server, MessageSquare, Settings, LogOut, Zap, ChevronRight } from 'lucide-react'
import safoziLogo from '../assets/safoziLogo.png'
import { useAuthStore } from '../store/authStore'

const navItems = [
  { to: '/dashboard', icon: LayoutDashboard, label: 'Dashboard',        desc: 'Vue générale' },
  { to: '/vms',       icon: Server,          label: 'Mes VMs',          desc: 'Gérer les serveurs' },
  { to: '/chat',      icon: MessageSquare,   label: 'Assistant IA',     desc: 'Support intelligent' },
  { to: '/settings',  icon: Settings,        label: 'Paramètres',       desc: 'Compte & sécurité' },
]

function SidebarContent({ onClose }) {
  const logout   = useAuthStore((s) => s.logout)
  const user     = useAuthStore((s) => s.user)
  const navigate = useNavigate()

  const displayName = user
    ? `${user.firstName || ''} ${user.lastName || ''}`.trim() || user.email
    : ''
  const initials = displayName.split(' ').map((w) => w[0]).join('').toUpperCase().slice(0, 2)

  return (
    <div className="flex h-full flex-col">
      {/* Logo area (mobile only — desktop logo is in Navbar) */}
      <div className="flex items-center border-b border-brand-border/80 px-4 py-3.5 lg:hidden">
        <div className="bg-white/95 px-3 py-1.5">
          <img src={safoziLogo} alt="SAFOZi" className="h-6 object-contain" />
        </div>
      </div>

      {/* Nav */}
      <nav className="flex-1 overflow-y-auto px-3 py-4">
        <p className="px-4 pb-3 pt-1 text-2xs font-bold uppercase tracking-widest text-slate-600">Menu</p>

        <div className="flex flex-col gap-1.5">
          {navItems.map(({ to, icon: Icon, label, desc }) => (
            <NavLink
              key={to}
              to={to}
              onClick={onClose}
              className={({ isActive }) =>
                `group flex items-center gap-3 px-4 py-3.5 text-xs font-semibold transition-all ${
                  isActive
                    ? 'text-white bg-primary/10 border border-primary/20'
                    : 'text-slate-400 border border-transparent hover:text-white hover:bg-white/5'
                }`
              }
            >
              {({ isActive }) => (
                <>
                  <div className={`flex h-8 w-8 items-center justify-center shrink-0 transition-all ${
                    isActive ? 'bg-primary/20' : 'bg-white/5 group-hover:bg-white/10'
                  }`}>
                    <Icon className={`h-4 w-4 ${isActive ? 'text-primary' : 'text-slate-500 group-hover:text-slate-300'}`} />
                  </div>
                  <div className="flex-1 min-w-0">
                    <p className={isActive ? 'text-white' : ''}>{label}</p>
                    <p className="text-2xs font-normal text-slate-600 truncate mt-0.5">{desc}</p>
                  </div>
                  {isActive && <ChevronRight className="h-3 w-3 text-primary shrink-0" />}
                </>
              )}
            </NavLink>
          ))}
        </div>

        {/* Divider */}
        <div className="my-4 h-px bg-brand-border-soft" />

        {/* Plan badge */}
        <div className="mx-0.5 border border-primary/15 bg-brand-surface p-3">
          <div className="flex items-center gap-2 mb-2">
            <Zap className="h-3.5 w-3.5 text-primary" />
            <span className="text-xs font-bold text-white">Safozi Pro</span>
          </div>
          <p className="text-2xs text-slate-500 leading-relaxed mb-2">
            Accédez à toutes les fonctionnalités cloud de Safozi.
          </p>
          <div className="h-1 bg-brand-border-soft">
            <div className="h-1 w-2/3 bg-[linear-gradient(90deg,#2E9EE0,#5CC8F7)]" />
          </div>
          <p className="mt-1 text-2xs text-slate-600">2/3 ressources utilisées</p>
        </div>
      </nav>

      {/* User + logout */}
      <div className="border-t border-brand-border/80">
        <div className="flex items-center gap-2.5 px-4 py-3">
          <div className="flex h-8 w-8 shrink-0 items-center justify-center rounded-full bg-[linear-gradient(135deg,#2E9EE0,#5CC8F7)] text-xs font-bold text-white">
            {initials || '?'}
          </div>
          <div className="flex-1 min-w-0">
            <p className="text-xs font-semibold text-white truncate">{displayName || 'Utilisateur'}</p>
            <p className="text-2xs text-slate-500 truncate">{user?.email}</p>
          </div>
          <button
            onClick={() => { logout(); navigate('/') }}
            className="flex h-7 w-7 items-center justify-center text-slate-500 hover:bg-crit/10 hover:text-crit transition-colors shrink-0"
            title="Déconnexion"
          >
            <LogOut className="h-3.5 w-3.5" />
          </button>
        </div>
      </div>
    </div>
  )
}

export default function Sidebar({ mobileOpen, onClose }) {
  const h = 'calc(100vh - 4rem)'

  return (
    <>
      {/* Desktop: sticky, IN FLEX FLOW → flex-1 on main works correctly */}
      <aside className="hidden lg:flex w-64 shrink-0 flex-col sticky top-16 self-start border-r border-brand-border/80 bg-brand-bg" style={{ height: h }}>
        <SidebarContent />
      </aside>

      {/* Mobile: fixed overlay */}
      <aside
        className={`fixed left-0 top-16 z-40 flex w-64 flex-col border-r border-brand-border/80 bg-brand-bg lg:hidden transition-transform duration-300 ease-in-out ${
          mobileOpen ? 'translate-x-0' : '-translate-x-full'
        }`}
        style={{ height: h }}
      >
        <SidebarContent onClose={onClose} />
      </aside>
    </>
  )
}