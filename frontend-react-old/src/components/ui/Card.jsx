import { cx } from './cx'

export function Card({ className = '', children, ...props }) {
  return (
    <div className={cx('border border-brand-border bg-brand-surface', className)} {...props}>
      {children}
    </div>
  )
}

export function CardHeader({ className = '', children, ...props }) {
  return (
    <div
      className={cx('flex items-center justify-between border-b border-brand-border px-4 py-3', className)}
      {...props}
    >
      {children}
    </div>
  )
}

export function CardTitle({ icon: Icon, children, iconClassName = 'text-primary' }) {
  return (
    <div className="flex items-center gap-2">
      {Icon && <Icon className={cx('h-4 w-4', iconClassName)} />}
      <span className="text-xs font-bold text-white">{children}</span>
    </div>
  )
}