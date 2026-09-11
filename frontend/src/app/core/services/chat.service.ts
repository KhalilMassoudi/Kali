import { Injectable, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { firstValueFrom } from 'rxjs';
import { ChatMessage, Conversation, ConversationMessage, MessageRequest, MessageResponse } from '../models/chat.model';

@Injectable({ providedIn: 'root' })
export class ChatService {
  private readonly http = inject(HttpClient);

  private readonly _messages = signal<ChatMessage[]>([]);
  private readonly _loading = signal(false);
  private readonly _error = signal<string | null>(null);
  private readonly _conversations = signal<Conversation[]>([]);
  private readonly _activeConversationId = signal<number | null>(null);
  private readonly _waitElapsedMs = signal(0);
  private waitTimer?: ReturnType<typeof setInterval>;

  readonly messages = this._messages.asReadonly();
  readonly loading = this._loading.asReadonly();
  readonly error = this._error.asReadonly();
  readonly conversations = this._conversations.asReadonly();
  readonly activeConversationId = this._activeConversationId.asReadonly();
  readonly waitElapsedMs = this._waitElapsedMs.asReadonly();

  /** À l'ouverture de la page : charge la liste des conversations pour l'historique,
   *  sans en rouvrir une automatiquement (on démarre sur une conversation vierge, comme
   *  n'importe quel client de chat classique). */
  async init(): Promise<void> {
    await this.loadConversations();
  }

  async loadConversations(): Promise<void> {
    try {
      const data = await firstValueFrom(this.http.get<Conversation[]>('/api/chat/conversations'));
      this._conversations.set(Array.isArray(data) ? data : []);
    } catch {
      // best-effort — la sidebar reste vide si l'appel échoue
    }
  }

  async openConversation(conversationId: number): Promise<void> {
    this._loading.set(true);
    this._error.set(null);
    try {
      const data = await firstValueFrom(
        this.http.get<ConversationMessage[]>(`/api/chat/conversations/${conversationId}/messages`),
      );
      const msgs: ChatMessage[] = (Array.isArray(data) ? data : []).map((m) => ({
        id: m.id,
        role: m.role === 'ASSISTANT' ? 'assistant' : 'user',
        content: m.content,
        timestamp: m.createdAt,
      }));
      this._messages.set(msgs);
      this._activeConversationId.set(conversationId);
    } catch (err: any) {
      this._error.set(err?.error?.message || 'Impossible de charger cette conversation');
    } finally {
      this._loading.set(false);
    }
  }

  /** Démarre une nouvelle conversation vierge (le prochain message crée une nouvelle
   *  conversation côté backend). */
  startNewConversation(): void {
    this._messages.set([]);
    this._activeConversationId.set(null);
    this._error.set(null);
  }

  async sendMessage(userId: number, content: string): Promise<void> {
    const userMsg: ChatMessage = { id: Date.now(), role: 'user', content, timestamp: new Date().toISOString() };
    this._messages.update((m) => [...m, userMsg]);
    this._loading.set(true);
    this._error.set(null);
    this.startWaitTimer();
    try {
      const body: MessageRequest = { userId, message: content, conversationId: this._activeConversationId() ?? undefined };
      const data = await firstValueFrom(this.http.post<MessageResponse>('/api/chat/message', body));
      this._activeConversationId.set(data.conversationId);
      const botMsg: ChatMessage = {
        id: Date.now() + 1,
        role: 'assistant',
        content: data.message,
        timestamp: new Date().toISOString(),
      };
      this._messages.update((m) => [...m, botMsg]);
      // Rafraîchit la sidebar : nouveau titre / nouvelle conversation / ordre par date de mise à jour.
      this.loadConversations();
    } catch (err: any) {
      const errContent = err?.error?.message || 'Erreur de communication';
      const errMsg: ChatMessage = { id: Date.now() + 1, role: 'error', content: errContent, timestamp: new Date().toISOString() };
      this._messages.update((m) => [...m, errMsg]);
      this._error.set(errContent);
    } finally {
      this._loading.set(false);
      this.stopWaitTimer();
    }
  }

  private startWaitTimer(): void {
    this._waitElapsedMs.set(0);
    const start = Date.now();
    this.waitTimer = setInterval(() => this._waitElapsedMs.set(Date.now() - start), 1000);
  }

  private stopWaitTimer(): void {
    if (this.waitTimer) clearInterval(this.waitTimer);
    this._waitElapsedMs.set(0);
  }
}
