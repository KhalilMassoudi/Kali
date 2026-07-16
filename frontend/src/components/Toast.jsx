import { X, CheckCircle, AlertCircle, Info } from 'lucide-react'
import { useToastStore } from '../store/toastStore'

const CONFIGS = {
  error:   { icon: AlertCircle, border: 'border-red-500/40',   bg: 'bg-red-500/10',   icon_color: 'text-red-400',   text: 'text-red-300' },
  success: { icon: CheckCircle, border: 'border-green-500/40', bg: 'bg-green-500/10', icon_color: 'text-green-400', text: 'text-green-300' },
  info:    { icon: Info,        border: 'border-blue-500/40',  bg: 'bg-blue-500/10',  icon_color: 'text-blue-400',  text: 'text-blue-300' },
}

function ToastItem({ toast }) {
  const remove = useToastStore((s) => s.removeToast)
  const cfg = CONFIGS[toast.type] || CONFIGS.error
  const Icon = cfg.icon

  return (
    <div className={`flex items-start gap-3 rounded-xl border px-4 py-3 shadow-2xl backdrop-blur-sm ${cfg.border} ${cfg.bg}`}
      style={{ minWidth: '280px', maxWidth: '400px' }}>
      <Icon className={`mt-0.5 h-5 w-5 shrink-0 ${cfg.icon_color}`} />
      <p className={`flex-1 text-sm font-medium ${cfg.text}`}>{toast.message}</p>
      <button
        onClick={() => remove(toast.id)}
        className="text-gray-500 hover:text-gray-300 transition-colors shrink-0"
      >
        <X className="h-4 w-4" />
      </button>
    </div>
  )
}

export default function Toast() {
  const toasts = useToastStore((s) => s.toasts)
  if (toasts.length === 0) return null

  return (
    <div className="fixed bottom-6 right-6 z-[100] flex flex-col gap-2">
      {toasts.map((t) => <ToastItem key={t.id} toast={t} />)}
    </div>
  )
}