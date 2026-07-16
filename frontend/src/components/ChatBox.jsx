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
      <div className="flex gap-3 justify-start">
        <div className="flex h-8 w-8 shrink-0 items-center justify-center rounded-full bg-red-500/20">
          <AlertCircle className="h-4 w-4 text-red-400" />
        </div>
        <div
          className="max-w-[80%] rounded-2xl rounded-tl-sm px-4 py-3"
          style={{ border: '1px solid rgba(239,68,68,0.3)', backgroundColor: 'rgba(239,68,68,0.08)' }}
        >
          <p className="text-sm text-red-300">{msg.content}</p>
        </div>
      </div>
    )
  }

  if (isUser) {
    return (
      <div className="flex gap-3 flex-row-reverse">
        <div className="flex h-8 w-8 shrink-0 items-center justify-center rounded-full text-xs font-bold text-white"
          style={{ background: 'linear-gradient(135deg, #00A3FF, #00D9FF)' }}>
          <User className="h-4 w-4" />
        </div>
        <div className="max-w-[80%]">
          <div
            className="rounded-2xl rounded-tr-sm px-4 py-3"
            style={{ background: 'linear-gradient(135deg, #00A3FF, #0078C8)', boxShadow: '0 2px 12px rgba(0,163,255,0.2)' }}
          >
            <p className="text-sm text-white whitespace-pre-wrap break-words leading-relaxed">{msg.content}</p>
          </div>
          {time && <p className="mt-1 text-right text-[10px] text-gray-600">{time}</p>}
        </div>
      </div>
    )
  }

  return (
    <div className="flex gap-3 flex-row">
      <div className="flex h-8 w-8 shrink-0 items-center justify-center rounded-full"
        style={{ backgroundColor: '#162030', border: '1px solid #1E3A5F' }}>
        <Bot className="h-4 w-4 text-accent" />
      </div>
      <div className="max-w-[80%]">
        <div
          className="rounded-2xl rounded-tl-sm px-4 py-3"
          style={{ backgroundColor: '#162030', border: '1px solid #1E3A5F' }}
        >
          <p className="text-sm text-gray-200 whitespace-pre-wrap break-words leading-relaxed">{msg.content}</p>
        </div>
        {time && <p className="mt-1 text-[10px] text-gray-600">{time}</p>}
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
      <div className="flex flex-col items-center justify-center h-full gap-6 text-center py-10">
        <div
          className="flex h-20 w-20 items-center justify-center rounded-3xl"
          style={{ background: 'linear-gradient(135deg, #00A3FF20, #00D9FF20)', border: '1px solid #1E3A5F' }}
        >
          <Cloud className="h-10 w-10 text-primary" />
        </div>
        <div>
          <h3 className="text-lg font-bold text-white">Assistant Safozi IA</h3>
          <p className="mt-2 text-sm text-gray-400 max-w-xs">
            Gérez votre infrastructure cloud en langage naturel. Posez vos questions ou donnez des instructions.
          </p>
        </div>
        <div className="flex flex-wrap justify-center gap-2">
          {['Montre mes VMs', 'Crée une VM Ubuntu 2 Go', 'Status de mon infrastructure', 'Aide-moi avec ma config'].map((s) => (
            <span
              key={s}
              className="rounded-full px-4 py-1.5 text-xs text-gray-400 cursor-default"
              style={{ border: '1px solid #1E3A5F', backgroundColor: '#162030' }}
            >
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
        <div className="flex gap-3 flex-row">
          <div className="flex h-8 w-8 shrink-0 items-center justify-center rounded-full"
            style={{ backgroundColor: '#162030', border: '1px solid #1E3A5F' }}>
            <Bot className="h-4 w-4 text-accent" />
          </div>
          <div
            className="flex items-center gap-1.5 rounded-2xl rounded-tl-sm px-4 py-3"
            style={{ backgroundColor: '#162030', border: '1px solid #1E3A5F' }}
          >
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