import { Loader2 } from 'lucide-react'
import { cx } from './cx'

const VARIANTS = {
  primary:
    'text-white bg-[linear-gradient(135deg,#2E9EE0,#5CC8F7)] shadow-[0_4px_16px_rgba(46,158,224,0.3)] hover:opacity-90 disabled:opacity-60 disabled:shadow-none',
  secondary:
    'text-slate-400 bg-brand-surface border border-brand-border hover:text-white disabled:opacity-50',
  ghost:
    'text-slate-400 hover:text-white hover:bg-white/5 disabled:opacity-40',
  danger:
    'text-white bg-crit hover:opacity-90 disabled:opacity-50',
  outline:
    'text-slate-400 border border-brand-border hover:text-white disabled:opacity-40',
}

const SIZES = {
  sm: 'gap-1.5 px-3 py-1.5 text-2xs',
  md: 'gap-1.5 px-4 py-2 text-xs',
  lg: 'gap-2 px-5 py-3 text-sm',
  icon: 'h-6 w-6',
  iconMd: 'h-8 w-8',
}

export default function Button({
  variant = 'primary',
  size = 'md',
  loading = false,
  disabled = false,
  className = '',
  children,
  ...props
}) {
  return (
    <button
      disabled={disabled || loading}
      className={cx(
        'inline-flex shrink-0 items-center justify-center font-bold transition-all focus:outline-none focus-visible:ring-2 focus-visible:ring-primary/40',
        VARIANTS[variant],
        SIZES[size],
        className
      )}
      {...props}
    >
      {loading ? <Loader2 className="h-3.5 w-3.5 animate-spin" /> : children}
    </button>
  )
}