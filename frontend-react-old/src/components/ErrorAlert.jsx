import { XCircle, X } from 'lucide-react'

export default function ErrorAlert({ message, onClose }) {
  if (!message) return null
  return (
    <div className="flex items-start gap-3 border border-crit/35 bg-crit/[0.08] px-4 py-3.5">
      <XCircle className="mt-0.5 h-5 w-5 shrink-0 text-crit" />
      <p className="flex-1 text-sm font-medium text-crit">{message}</p>
      {onClose && (
        <button onClick={onClose} className="shrink-0 text-crit/60 transition-colors hover:text-crit">
          <X className="h-4 w-4" />
        </button>
      )}
    </div>
  )
}