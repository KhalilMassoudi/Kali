import { XCircle, X } from 'lucide-react'

export default function ErrorAlert({ message, onClose }) {
  if (!message) return null
  return (
    <div className="flex items-start gap-3 rounded-xl px-4 py-3.5"
      style={{ border: '1px solid rgba(239,68,68,0.35)', backgroundColor: 'rgba(239,68,68,0.08)' }}>
      <XCircle className="mt-0.5 h-5 w-5 shrink-0 text-red-400" />
      <p className="flex-1 text-sm font-medium text-red-300">{message}</p>
      {onClose && (
        <button onClick={onClose} className="text-red-400/60 hover:text-red-300 transition-colors shrink-0">
          <X className="h-4 w-4" />
        </button>
      )}
    </div>
  )
}