import { useEffect, useRef, useState } from 'react'
import { Send, Trash2, Bot, Sparkles } from 'lucide-react'
import { useAuthStore } from '../store/authStore'
import { useChatStore } from '../store/chatStore'
import ChatBox from '../components/ChatBox'
import { Card } from '../components/ui/Card'
import Button from '../components/ui/Button'

const SUGGESTIONS = [
  'Crée une VM Ubuntu avec 2 vCPUs et 4 GB de RAM',
  'Montre moi l\'état de mes VMs',
  'Arrête toutes mes VMs en erreur',
  'Quelle VM consomme le plus de ressources ?',
]

export default function ChatPage() {
  const user = useAuthStore((s) => s.user)
  const { messages, loading, sendMessage, getConversations, clearMessages } = useChatStore()
  const [input, setInput] = useState('')
  const textareaRef = useRef(null)

  useEffect(() => {
    if (user?.id) getConversations(user.id)
  }, [user?.id])

  const handleSend = async (text = input) => {
    const trimmed = text.trim()
    if (!trimmed || loading) return
    setInput('')
    if (textareaRef.current) {
      textareaRef.current.style.height = 'auto'
    }
    await sendMessage(user?.id, trimmed)
  }

  const handleKey = (e) => {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault()
      handleSend()
    }
  }

  const handleInput = (e) => {
    setInput(e.target.value)
    e.target.style.height = 'auto'
    e.target.style.height = `${Math.min(e.target.scrollHeight, 160)}px`
  }

  const isEmpty = messages.length === 0

  return (
    <div className="flex flex-col" style={{ height: 'calc(100vh - 4rem)' }}>
      <div className="flex flex-col flex-1 px-6 py-6 min-h-0 w-full">
        <Card className="flex flex-col flex-1 overflow-hidden min-h-0">
          {/* Chat header */}
          <div className="flex items-center justify-between px-6 py-4 shrink-0 border-b border-brand-border">
            <div className="flex items-center gap-3">
              <div className="flex h-9 w-9 items-center justify-center bg-primary/10 border border-primary/20">
                <Bot className="h-[1.125rem] w-[1.125rem] text-primary" />
              </div>
              <div>
                <h3 className="text-sm font-bold text-white">Assistant Safozi IA</h3>
                <div className="flex items-center gap-1.5 mt-0.5">
                  <span className="h-1.5 w-1.5 rounded-full bg-good animate-pulse" />
                  <p className="text-[10px] text-slate-500">En ligne — Gérez votre infrastructure en langage naturel</p>
                </div>
              </div>
            </div>
            {!isEmpty && (
              <Button variant="outline" size="sm" onClick={clearMessages} className="hover:text-crit">
                <Trash2 className="h-3 w-3" /> Effacer
              </Button>
            )}
          </div>

          {/* Messages or empty state */}
          <div className="flex-1 overflow-y-auto">
            {isEmpty ? (
              <div className="flex flex-col items-center justify-center h-full gap-6 px-6 py-10 text-center">
                <div className="flex h-16 w-16 items-center justify-center bg-[linear-gradient(135deg,rgba(46,158,224,0.15),rgba(92,200,247,0.05))] border border-primary/20">
                  <Sparkles className="h-8 w-8 text-primary" />
                </div>
                <div>
                  <p className="text-base font-bold text-white">Comment puis-je vous aider ?</p>
                  <p className="mt-1 text-xs text-slate-500 max-w-sm">
                    Décrivez ce que vous souhaitez faire avec votre infrastructure cloud en langage naturel.
                  </p>
                </div>
                <div className="grid grid-cols-1 sm:grid-cols-2 gap-2 w-full max-w-xl">
                  {SUGGESTIONS.map((s) => (
                    <button
                      key={s}
                      onClick={() => handleSend(s)}
                      className="text-left px-5 py-4 text-xs text-slate-300 hover:text-white transition-all hover:-translate-y-0.5 border border-brand-border bg-brand-surface2 hover:border-primary/30"
                    >
                      <span className="text-primary mr-1.5">→</span>{s}
                    </button>
                  ))}
                </div>
              </div>
            ) : (
              <div className="px-6">
                <ChatBox messages={messages} loading={loading} />
              </div>
            )}
          </div>

          {/* Input */}
          <div className="shrink-0 p-5 border-t border-brand-border">
            <div className="flex items-end gap-3 p-4 transition-all border border-brand-border bg-brand-surface2 focus-within:border-primary/40">
              <textarea
                ref={textareaRef}
                value={input}
                onChange={handleInput}
                onKeyDown={handleKey}
                placeholder="Posez une question ou donnez une instruction... (Entrée pour envoyer)"
                rows={1}
                disabled={loading}
                className="flex-1 resize-none bg-transparent text-sm text-white placeholder-slate-600 focus:outline-none disabled:opacity-50"
                style={{ maxHeight: '160px' }}
              />
              <Button
                variant="primary"
                size="iconMd"
                onClick={() => handleSend()}
                disabled={!input.trim() || loading}
              >
                <Send className="h-3.5 w-3.5" />
              </Button>
            </div>
            <p className="mt-1.5 text-center text-[10px] text-slate-700">Shift+Entrée pour nouvelle ligne</p>
          </div>
        </Card>
      </div>
    </div>
  )
}