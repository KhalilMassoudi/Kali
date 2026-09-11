import { cx } from './cx'

const fieldBase =
  'w-full bg-brand-surface2 border border-brand-border px-4 py-3 text-sm text-white placeholder-slate-500 transition-all focus:outline-none focus:border-primary/60 focus:ring-2 focus:ring-primary/20'
const fieldError = 'border-crit/70 focus:border-crit/70 focus:ring-crit/20'

export function Field({ label, hint, error, icon: Icon, className = '', children }) {
  return (
    <div className={className}>
      {label && (
        <label className="mb-2 flex items-center gap-1.5 text-sm font-semibold text-slate-300">
          {Icon && <Icon className="h-4 w-4 text-slate-500" />}
          {label}
        </label>
      )}
      {children}
      {error ? (
        <p className="mt-1.5 text-xs text-crit">{error}</p>
      ) : hint ? (
        <p className="mt-1.5 text-xs text-slate-600">{hint}</p>
      ) : null}
    </div>
  )
}

export function Input({ error, className = '', ...props }) {
  return <input className={cx(fieldBase, error && fieldError, className)} {...props} />
}

export function Select({ error, className = '', children, ...props }) {
  return (
    <select className={cx(fieldBase, error && fieldError, className)} {...props}>
      {children}
    </select>
  )
}