import { cx } from './cx'

const VARIANTS = {
  primary: 'text-primary bg-primary/10',
  good: 'text-good bg-good/10',
  warn: 'text-warn bg-warn/10',
  crit: 'text-crit bg-crit/10',
  neutral: 'text-slate-500 bg-white/5',
}

export default function Badge({ variant = 'neutral', dot = false, className = '', children }) {
  return (
    <span
      className={cx(
        'inline-flex items-center gap-1.5 px-2 py-0.5 text-2xs font-bold whitespace-nowrap',
        VARIANTS[variant],
        className
      )}
    >
      {dot && <span className={cx('h-1.5 w-1.5 shrink-0 animate-pulse', VARIANTS[variant].split(' ')[0].replace('text-', 'bg-'))} />}
      {children}
    </span>
  )
}