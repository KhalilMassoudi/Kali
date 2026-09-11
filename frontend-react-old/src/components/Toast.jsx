import { X, CheckCircle, AlertCircle, Info } from 'lucide-react'
import { useToastStore } from '../store/toastStore'

const CONFIGS = {
  error:   { icon: AlertCircle, border: 'border-crit/40',    bg: 'bg-crit/10',    icon_color: 'text-crit',    text: 'text-crit' },
  success: { icon: CheckCircle, border: 'border-good/40',    bg: 'bg-good/10',    icon_color: 'text-good',    text: 'text-good' },
  info:    { icon: Info,        border: 'border-primary/40', bg: 'bg-primary/10', icon_color: 'text-primary', text: 'text-accent' },
}

function ToastItem({ toast }) {
  const remove = useToastStore((s) => s.removeToast)
  const cfg = CONFIGS[toast.type] || CONFIGS.error
  const Icon = cfg.icon

  return (
    <div className={`flex w-[280px] max-w-[400px] items-start gap-3 border px-4 py-3 shadow-2xl backdrop-blur-sm ${cfg.border} ${cfg.bg}`}>
      <Icon className={`mt-0.5 h-5 w-5 shrink-0 ${cfg.icon_color}`} />
      <p className={`flex-1 text-sm font-medium ${cfg.text}`}>{toast.message}</p>
      <button
        onClick={() => remove(toast.id)}
        className="text-slate-500 hover:text-slate-300 transition-colors shrink-0"
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