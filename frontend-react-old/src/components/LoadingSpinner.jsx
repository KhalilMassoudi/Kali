export default function LoadingSpinner({ size = 'md', text = '' }) {
  const sizes = { sm: 'h-4 w-4', md: 'h-8 w-8', lg: 'h-12 w-12' }
  return (
    <div className="flex flex-col items-center justify-center gap-3">
      <div className={`${sizes[size]} animate-spin rounded-full border-2 border-brand-border border-t-primary`} />
      {text && <p className="text-sm text-slate-400">{text}</p>}
    </div>
  )
}