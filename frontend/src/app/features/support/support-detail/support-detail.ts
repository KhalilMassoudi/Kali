import { Component, computed, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { ButtonModule } from 'primeng/button';
import { TagModule } from 'primeng/tag';
import { TextareaModule } from 'primeng/textarea';
import { AuthService } from '../../../core/services/auth.service';
import { SupportService } from '../../../core/services/support.service';
import { TicketPriority, TicketStatus } from '../../../core/models/support.model';

const STATUS_LABELS: Record<TicketStatus, string> = {
  OPEN: 'Ouvert',
  IN_PROGRESS: 'En cours',
  WAITING_RESPONSE: 'En attente de votre réponse',
  RESOLVED: 'Résolu',
  CLOSED: 'Fermé',
};

const STATUS_SEVERITIES: Record<TicketStatus, 'info' | 'warn' | 'success' | 'secondary'> = {
  OPEN: 'info',
  IN_PROGRESS: 'warn',
  WAITING_RESPONSE: 'warn',
  RESOLVED: 'success',
  CLOSED: 'secondary',
};

const PRIORITY_LABELS: Record<TicketPriority, string> = {
  LOW: 'Faible',
  MEDIUM: 'Moyenne',
  HIGH: 'Haute',
  CRITICAL: 'Critique',
};

@Component({
  selector: 'app-support-detail',
  imports: [CommonModule, RouterLink, FormsModule, ButtonModule, TagModule, TextareaModule],
  templateUrl: './support-detail.html',
  styleUrl: './support-detail.scss',
})
export class SupportDetail {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly auth = inject(AuthService);
  readonly support = inject(SupportService);

  readonly ticket = this.support.currentTicket;
  readonly comments = this.support.comments;
  readonly attachments = this.support.attachments;
  readonly loading = this.support.loading;
  readonly error = this.support.error;

  readonly replyText = signal('');
  readonly replyFile = signal<File | null>(null);
  readonly sending = signal(false);
  readonly closing = signal(false);

  readonly ticketAttachments = computed(() => this.attachments().filter((a) => a.commentId === null));
  attachmentsFor(commentId: number) {
    return this.attachments().filter((a) => a.commentId === commentId);
  }

  onReplyFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    this.replyFile.set(input.files?.[0] ?? null);
  }

  formatSize(bytes: number): string {
    return bytes < 1024 * 1024 ? `${Math.round(bytes / 1024)} Ko` : `${(bytes / (1024 * 1024)).toFixed(1)} Mo`;
  }

  download(id: number, filename: string): void {
    this.support.downloadAttachment(id, filename);
  }

  readonly isClosed = computed(() => this.ticket()?.status === 'CLOSED');
  readonly myUserId = computed(() => this.auth.user()?.id ?? null);

  readonly statusLabel = (s: TicketStatus) => STATUS_LABELS[s] ?? s;
  readonly statusSeverity = (s: TicketStatus) => STATUS_SEVERITIES[s] ?? 'info';
  readonly priorityLabel = (p: TicketPriority) => PRIORITY_LABELS[p] ?? p;

  private readonly ticketId = Number(this.route.snapshot.paramMap.get('id'));

  constructor() {
    this.support.getTicket(this.ticketId).catch(() => this.router.navigate(['/support']));
    this.support.getComments(this.ticketId);
    this.support.getAttachments(this.ticketId);
  }

  async sendReply(): Promise<void> {
    const content = this.replyText().trim();
    if (!content) return;
    this.sending.set(true);
    try {
      const comment = await this.support.addComment(this.ticketId, content);
      const file = this.replyFile();
      if (file) {
        await this.support.uploadAttachment(this.ticketId, file, comment.id);
        this.replyFile.set(null);
      }
      this.replyText.set('');
    } finally {
      this.sending.set(false);
    }
  }

  async close(): Promise<void> {
    this.closing.set(true);
    try {
      await this.support.closeTicket(this.ticketId);
    } finally {
      this.closing.set(false);
    }
  }
}
