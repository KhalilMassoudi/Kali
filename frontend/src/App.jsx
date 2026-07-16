import { useEffect, useState } from 'react'
import { BrowserRouter, Routes, Route, Navigate, Outlet } from 'react-router-dom'
import { useAuthStore } from './store/authStore'
import Navbar from './components/Navbar'
import Sidebar from './components/Sidebar'
import Toast from './components/Toast'
import LoginPage from './pages/LoginPage'
import RegisterPage from './pages/RegisterPage'
import DashboardPage from './pages/DashboardPage'
import VmsPage from './pages/VmsPage'
import ChatPage from './pages/ChatPage'
import SettingsPage from './pages/SettingsPage'

function AuthInitializer({ children }) {
  const { isLoggedIn, getMe, user } = useAuthStore()
  useEffect(() => {
    if (isLoggedIn && !user) getMe()
  }, [isLoggedIn])
  return children
}

// Layout for authenticated pages — uses Outlet so pages render without AppLayout
function ProtectedLayout() {
  const isLoggedIn = useAuthStore((s) => s.isLoggedIn)
  const [mobileOpen, setMobileOpen] = useState(false)

  if (!isLoggedIn) return <Navigate to="/" replace />

  return (
    <div className="min-h-screen bg-brand-bg">
      {/* Navbar: fixed top, full width */}
      <Navbar onMenuToggle={() => setMobileOpen((o) => !o)} />

      {/* Body: flex row, pushed below navbar */}
      <div className="flex" style={{ paddingTop: '4rem' /* h-16 */ }}>
        {/* Sidebar handles desktop (sticky, in flow) + mobile (fixed overlay) */}
        <Sidebar mobileOpen={mobileOpen} onClose={() => setMobileOpen(false)} />

        {/* Main: takes all remaining space — NO ml- needed with sticky sidebar */}
        <main className="flex-1 min-w-0">
          <Outlet />
        </main>
      </div>

      {/* Mobile backdrop */}
      {mobileOpen && (
        <div
          className="fixed inset-0 z-30 bg-black/60 lg:hidden"
          onClick={() => setMobileOpen(false)}
        />
      )}

      <Toast />
    </div>
  )
}

export default function App() {
  return (
    <BrowserRouter>
      <AuthInitializer>
        <Routes>
          {/* Public */}
          <Route path="/"         element={<LoginPage />} />
          <Route path="/register" element={<RegisterPage />} />

          {/* Protected — ProtectedLayout wraps all authenticated pages */}
          <Route element={<ProtectedLayout />}>
            <Route path="/dashboard" element={<DashboardPage />} />
            <Route path="/vms"       element={<VmsPage />} />
            <Route path="/chat"      element={<ChatPage />} />
            <Route path="/settings"  element={<SettingsPage />} />
          </Route>

          <Route path="*" element={<Navigate to="/" replace />} />
        </Routes>
      </AuthInitializer>
    </BrowserRouter>
  )
}