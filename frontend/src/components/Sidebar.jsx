import { NavLink, useNavigate } from 'react-router-dom'
import { LayoutDashboard, Server, MessageSquare, Settings, LogOut, Zap, ChevronRight } from 'lucide-react'
import { useAuthStore } from '../store/authStore'

const navItems = [
  { to: '/dashboard', icon: LayoutDashboard, label: 'Dashboard',        desc: 'Vue générale' },
  { to: '/vms',       icon: Server,          label: 'Mes VMs',          desc: 'Gérer les serveurs' },
  { to: '/chat',      icon: MessageSquare,   label: 'Assistant IA',     desc: 'Support intelligent' },
  { to: '/settings',  icon: Settings,        label: 'Paramètres',       desc: 'Compte & sécurité' },
]

const sStyle = {
  borderRight: '1px solid rgba(26,51,84,0.8)',
  backgroundColor: '#080D14',
}

function SidebarContent({ onClose }) {
  const logout   = useAuthStore((s) => s.logout)
  const user     = useAuthStore((s) => s.user)
  const navigate = useNavigate()

  const displayName = user
    ? `${user.firstName || ''} ${user.lastName || ''}`.trim() || user.email
    : ''
  const initials = displayName.split(' ').map((w) => w[0]).join('').toUpperCase().slice(0, 2)

  return (
    <div className="flex flex-col h-full">
      {/* Logo area (mobile only — desktop logo is in Navbar) */}
      <div className="lg:hidden flex items-center gap-2.5 px-5 py-4" style={{ borderBottom: '1px solid rgba(26,51,84,0.8)' }}>
        <div className="flex h-7 w-7 items-center justify-center rounded-lg shrink-0"
          style={{ background: 'linear-gradient(135deg, #0EA5E9, #38BDF8)' }}>
          <Zap className="h-3.5 w-3.5 text-white" />
        </div>
        <span className="text-sm font-bold text-white">Safozi<span className="text-primary"> Cloud</span></span>
      </div>

      {/* Nav */}
      <nav className="flex-1 overflow-y-auto px-3 py-4">
        <p className="px-3 pb-2 pt-1 text-[10px] font-bold uppercase tracking-widest text-slate-600">Menu</p>

        {navItems.map(({ to, icon: Icon, label, desc }) => (
          <NavLink
            key={to}
            to={to}
            onClick={onClose}
            className={({ isActive }) =>
              `group flex items-center gap-3 px-3 py-2.5 rounded-xl text-xs font-semibold transition-all mb-0.5 ${
                isActive
                  ? 'text-white bg-primary/10 border border-primary/20'
                  : 'text-slate-400 border border-transparent hover:text-white hover:bg-white/5'
              }`
            }
          >
            {({ isActive }) => (
              <>
                <div className={`flex h-7 w-7 items-center justify-center rounded-lg shrink-0 transition-all ${
                  isActive ? 'bg-primary/20' : 'bg-white/5 group-hover:bg-white/10'
                }`}>
                  <Icon className={`h-3.5 w-3.5 ${isActive ? 'text-primary' : 'text-slate-500 group-hover:text-slate-300'}`} />
                </div>
                <div className="flex-1 min-w-0">
                  <p className={isActive ? 'text-white' : ''}>{label}</p>
                  <p className="text-[10px] font-normal text-slate-600 truncate">{desc}</p>
                </div>
                {isActive && <ChevronRight className="h-3 w-3 text-primary shrink-0" />}
              </>
            )}
          </NavLink>
        ))}

        {/* Divider */}
        <div className="my-4 h-px" style={{ backgroundColor: 'rgba(26,51,84,0.6)' }} />

        {/* Plan badge */}
        <div className="rounded-xl p-3 mx-0.5" style={{ backgroundColor: '#0E1825', border: '1px solid rgba(14,165,233,0.15)' }}>
          <div className="flex items-center gap-2 mb-2">
            <Zap className="h-3.5 w-3.5 text-primary" />
            <span className="text-xs font-bold text-white">Safozi Pro</span>
          </div>
          <p className="text-[10px] text-slate-500 leading-relaxed mb-2">
            Accédez à toutes les fonctionnalités cloud de Safozi.
          </p>
          <div className="h-1 rounded-full" style={{ backgroundColor: 'rgba(26,51,84,0.6)' }}>
            <div className="h-1 w-2/3 rounded-full" style={{ background: 'linear-gradient(90deg, #0EA5E9, #38BDF8)' }} />
          </div>
          <p className="mt-1 text-[10px] text-slate-600">2/3 ressources utilisées</p>
        </div>
      </nav>

      {/* User + logout */}
      <div style={{ borderTop: '1px solid rgba(26,51,84,0.8)' }}>
        <div className="flex items-center gap-2.5 px-4 py-3">
          <div
            className="flex h-8 w-8 items-center justify-center rounded-full text-xs font-bold text-white shrink-0"
            style={{ background: 'linear-gradient(135deg, #0EA5E9, #38BDF8)' }}
          >
            {initials || '?'}
          </div>
          <div className="flex-1 min-w-0">
            <p className="text-xs font-semibold text-white truncate">{displayName || 'Utilisateur'}</p>
            <p className="text-[10px] text-slate-500 truncate">{user?.email}</p>
          </div>
          <button
            onClick={() => { logout(); navigate('/') }}
            className="flex h-7 w-7 items-center justify-center rounded-lg text-slate-500 hover:bg-red-500/10 hover:text-red-400 transition-colors shrink-0"
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
      <aside className="hidden lg:flex w-64 shrink-0 flex-col sticky top-16 self-start" style={{ height: h, ...sStyle }}>
        <SidebarContent />
      </aside>

      {/* Mobile: fixed overlay */}
      <aside
        className={`fixed left-0 top-16 z-40 flex w-64 flex-col lg:hidden transition-transform duration-300 ease-in-out ${
          mobileOpen ? 'translate-x-0' : '-translate-x-full'
        }`}
        style={{ height: h, ...sStyle }}
      >
        <SidebarContent onClose={onClose} />
      </aside>
    </>
  )
}
