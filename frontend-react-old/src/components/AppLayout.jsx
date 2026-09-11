import { useState } from 'react'
import Sidebar from './Sidebar'
import Navbar from './Navbar'
import Toast from './Toast'

export default function AppLayout({ children }) {
  const [sidebarOpen, setSidebarOpen] = useState(false)

  return (
    <div className="min-h-screen bg-brand-bg">
      {/* Navbar: fixed top, full width, z-50 */}
      <Navbar onMenuToggle={() => setSidebarOpen((o) => !o)} />

      {/* Sidebar: fixed left, z-40 */}
      <Sidebar open={sidebarOpen} onClose={() => setSidebarOpen(false)} />

      {/* Mobile overlay */}
      {sidebarOpen && (
        <div
          className="fixed inset-0 z-30 bg-black/60 lg:hidden"
          onClick={() => setSidebarOpen(false)}
        />
      )}

      {/* Main content: pushed right by sidebar width + down by navbar height */}
      <main className="pt-20 ml-72 min-h-screen overflow-y-auto">
        <div className="max-w-7xl px-6 py-8 lg:px-8">
          {children}
        </div>
      </main>

      <Toast />
    </div>
  )
}