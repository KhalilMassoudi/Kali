import { useEffect, useRef } from 'react'
import { Bot, User, AlertCircle, Cloud } from 'lucide-react'

function Message({ msg }) {
  const isUser = msg.role === 'user'
  const isError = msg.role === 'error'
  const time = msg.timestamp
    ? new Date(msg.timestamp).toLocaleTimeString('fr-FR', { hour: '2-digit', minute: '2-digit' })
    : null

  if (isError) {
    return (
      <div className="flex justify-start gap-3">
        <div className="flex h-8 w-8 shrink-0 items-center justify-center bg-crit/20">
          <AlertCircle className="h-4 w-4 text-crit" />
        </div>
        <div className="max-w-[80%] border border-crit/30 bg-crit/[0.08] px-4 py-3">
          <p className="text-sm text-crit">{msg.content}</p>
        </div>
      </div>
    )
  }

  if (isUser) {
    return (
      <div className="flex flex-row-reverse gap-3">
        <div className="flex h-8 w-8 shrink-0 items-center justify-center rounded-full bg-[linear-gradient(135deg,#2E9EE0,#5CC8F7)] text-xs font-bold text-white">
          <User className="h-4 w-4" />
        </div>
        <div className="max-w-[80%]">
          <div className="bg-[linear-gradient(135deg,#2E9EE0,#0284C7)] px-4 py-3 shadow-[0_2px_12px_rgba(46,158,224,0.2)]">
            <p className="whitespace-pre-wrap break-words text-sm leading-relaxed text-white">{msg.content}</p>
          </div>
          {time && <p className="mt-1 text-right text-2xs text-slate-600">{time}</p>}
        </div>
      </div>
    )
  }

  return (
    <div className="flex flex-row gap-3">
      <div className="flex h-8 w-8 shrink-0 items-center justify-center rounded-full border border-brand-border bg-brand-surface2">
        <Bot className="h-4 w-4 text-accent" />
      </div>
      <div className="max-w-[80%]">
        <div className="border border-brand-border bg-brand-surface2 px-4 py-3">
          <p className="whitespace-pre-wrap break-words text-sm leading-relaxed text-slate-200">{msg.content}</p>
        </div>
        {time && <p className="mt-1 text-2xs text-slate-600">{time}</p>}
      </div>
    </div>
  )
}

export default function ChatBox({ messages, loading }) {
  const bottomRef = useRef(null)

  useEffect(() => {
    bottomRef.current?.scrollIntoView({ behavior: 'smooth' })
  }, [messages, loading])

  if (messages.length === 0 && !loading) {
    return (
      <div className="flex h-full flex-col items-center justify-center gap-6 py-10 text-center">
        <div className="flex h-20 w-20 items-center justify-center border border-brand-border bg-[linear-gradient(135deg,rgba(46,158,224,0.15),rgba(92,200,247,0.05))]">
          <Cloud className="h-10 w-10 text-primary" />
        </div>
        <div>
          <h3 className="text-lg font-bold text-white">Assistant Safozi IA</h3>
          <p className="mt-2 max-w-xs text-sm text-slate-400">
            Gérez votre infrastructure cloud en langage naturel. Posez vos questions ou donnez des instructions.
          </p>
        </div>
        <div className="flex flex-wrap justify-center gap-2">
          {['Montre mes VMs', 'Crée une VM Ubuntu 2 Go', 'Status de mon infrastructure', 'Aide-moi avec ma config'].map((s) => (
            <span key={s} className="cursor-default border border-brand-border bg-brand-surface2 px-4 py-1.5 text-xs text-slate-400">
              {s}
            </span>
          ))}
        </div>
      </div>
    )
  }

  return (
    <div className="flex flex-col gap-5 py-5">
      {messages.map((msg) => <Message key={msg.id} msg={msg} />)}
      {loading && (
        <div className="flex flex-row gap-3">
          <div className="flex h-8 w-8 shrink-0 items-center justify-center rounded-full border border-brand-border bg-brand-surface2">
            <Bot className="h-4 w-4 text-accent" />
          </div>
          <div className="flex items-center gap-1.5 border border-brand-border bg-brand-surface2 px-4 py-3">
            <span className="h-2 w-2 rounded-full bg-accent animate-bounce" style={{ animationDelay: '0ms' }} />
            <span className="h-2 w-2 rounded-full bg-accent animate-bounce" style={{ animationDelay: '150ms' }} />
            <span className="h-2 w-2 rounded-full bg-accent animate-bounce" style={{ animationDelay: '300ms' }} />
          </div>
        </div>
      )}
      <div ref={bottomRef} />
    </div>
  )
}