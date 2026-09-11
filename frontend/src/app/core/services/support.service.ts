import { Injectable, inject, signal } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { firstValueFrom } from 'rxjs';
import { CreateTicketRequest, Ticket, TicketAttachment, TicketComment, TicketStatus } from '../models/support.model';

@Injectable({ providedIn: 'root' })
export class SupportService {
  private readonly http = inject(HttpClient);

  private readonly _tickets = signal<Ticket[]>([]);
  private readonly _currentTicket = signal<Ticket | null>(null);
  private readonly _comments = signal<TicketComment[]>([]);
  private readonly _attachments = signal<TicketAttachment[]>([]);
  private readonly _loading = signal(false);
  private readonly _error = signal<string | null>(null);
  private readonly _pendingCount = signal(0);

  readonly tickets = this._tickets.asReadonly();
  readonly currentTicket = this._currentTicket.asReadonly();
  readonly comments = this._comments.asReadonly();
  readonly attachments = this._attachments.asReadonly();
  readonly loading = this._loading.asReadonly();
  readonly error = this._error.asReadonly();
  /** Tickets currently WAITING_RESPONSE from this user — i.e. an agent replied and is waiting on them. */
  readonly pendingCount = this._pendingCount.asReadonly();

  async refreshPendingCount(userId: number): Promise<void> {
    try {
      const tickets = await firstValueFrom(this.http.get<Ticket[]>(`/api/support/tickets/user/${userId}`));
      this._pendingCount.set((Array.isArray(tickets) ? tickets : []).filter((t) => t.status === 'WAITING_RESPONSE').length);
    } catch {
      // Non-critical — badge just keeps its last known value.
    }
  }

  async fetchMyTickets(userId: number): Promise<void> {
    this._loading.set(true);
    this._error.set(null);
    try {
      const tickets = await firstValueFrom(this.http.get<Ticket[]>(`/api/support/tickets/user/${userId}`));
      this._tickets.set(Array.isArray(tickets) ? tickets : []);
    } catch (err: any) {
      this._error.set(err?.error?.message || 'Erreur lors du chargement des tickets');
    } finally {
      this._loading.set(false);
    }
  }

  async getTicket(id: number): Promise<void> {
    this._loading.set(true);
    this._error.set(null);
    try {
      const ticket = await firstValueFrom(this.http.get<Ticket>(`/api/support/tickets/${id}`));
      this._currentTicket.set(ticket);
    } catch (err: any) {
      this._error.set(err?.error?.message || 'Erreur lors du chargement du ticket');
      throw err;
    } finally {
      this._loading.set(false);
    }
  }

  async createTicket(data: CreateTicketRequest): Promise<Ticket> {
    this._error.set(null);
    try {
      return await firstValueFrom(this.http.post<Ticket>('/api/support/tickets', data));
    } catch (err: any) {
      this._error.set(err?.error?.message || 'Erreur lors de la création du ticket');
      throw err;
    }
  }

  async getComments(ticketId: number): Promise<void> {
    try {
      const comments = await firstValueFrom(this.http.get<TicketComment[]>(`/api/support/tickets/${ticketId}/comments`));
      this._comments.set(Array.isArray(comments) ? comments : []);
    } catch (err: any) {
      this._error.set(err?.error?.message || 'Erreur lors du chargement des messages');
    }
  }

  async addComment(ticketId: number, content: string): Promise<TicketComment> {
    this._error.set(null);
    try {
      const comment = await firstValueFrom(
        this.http.post<TicketComment>(`/api/support/tickets/${ticketId}/comments`, { content }),
      );
      this._comments.update((c) => [...c, comment]);
      return comment;
    } catch (err: any) {
      this._error.set(err?.error?.message || "Erreur lors de l'envoi du message");
      throw err;
    }
  }

  async updateStatus(id: number, status: TicketStatus): Promise<void> {
    this._error.set(null);
    try {
      const params = new HttpParams().set('status', status);
      const ticket = await firstValueFrom(
        this.http.patch<Ticket>(`/api/support/tickets/${id}/status`, {}, { params }),
      );
      this._currentTicket.set(ticket);
      this._tickets.update((tickets) => tickets.map((t) => (t.id === id ? ticket : t)));
    } catch (err: any) {
      this._error.set(err?.error?.message || 'Erreur lors de la mise à jour du ticket');
      throw err;
    }
  }

  closeTicket(id: number): Promise<void> {
    return this.updateStatus(id, 'CLOSED');
  }

  async getAttachments(ticketId: number): Promise<void> {
    try {
      const list = await firstValueFrom(
        this.http.get<TicketAttachment[]>(`/api/support/tickets/${ticketId}/attachments`),
      );
      this._attachments.set(Array.isArray(list) ? list : []);
    } catch {
      // Non-critical — attachment list just stays empty.
    }
  }

  async uploadAttachment(ticketId: number, file: File, commentId?: number): Promise<TicketAttachment> {
    this._error.set(null);
    try {
      const form = new FormData();
      form.append('file', file);
      let params = new HttpParams();
      if (commentId != null) params = params.set('commentId', String(commentId));
      const attachment = await firstValueFrom(
        this.http.post<TicketAttachment>(`/api/support/tickets/${ticketId}/attachments`, form, { params }),
      );
      this._attachments.update((list) => [...list, attachment]);
      return attachment;
    } catch (err: any) {
      this._error.set(err?.error?.message || "Erreur lors de l'envoi du fichier");
      throw err;
    }
  }

  async downloadAttachment(attachmentId: number, filename: string): Promise<void> {
    const blob = await firstValueFrom(
      this.http.get(`/api/support/attachments/${attachmentId}/download`, { responseType: 'blob' }),
    );
    const url = window.URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.download = filename;
    link.click();
    window.URL.revokeObjectURL(url);
  }

  clearError(): void {
    this._error.set(null);
  }
}
