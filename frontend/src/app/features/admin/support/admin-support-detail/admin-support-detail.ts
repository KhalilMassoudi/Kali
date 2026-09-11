import { Component, computed, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { ButtonModule } from 'primeng/button';
import { TagModule } from 'primeng/tag';
import { SelectModule } from 'primeng/select';
import { TextareaModule } from 'primeng/textarea';
import { AdminService } from '../../../../core/services/admin.service';
import { SupportService } from '../../../../core/services/support.service';
import { TicketPriority, TicketStatus } from '../../../../core/models/support.model';

const STATUS_LABELS: Record<TicketStatus, string> = {
  OPEN: 'Ouvert',
  IN_PROGRESS: 'En cours',
  WAITING_RESPONSE: 'Attente client',
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

const STATUS_OPTIONS: { label: string; value: TicketStatus }[] = (Object.keys(STATUS_LABELS) as TicketStatus[]).map(
  (value) => ({ label: STATUS_LABELS[value], value }),
);

@Component({
  selector: 'app-admin-support-detail',
  imports: [CommonModule, RouterLink, FormsModule, ButtonModule, TagModule, SelectModule, TextareaModule],
  templateUrl: './admin-support-detail.html',
  styleUrl: './admin-support-detail.scss',
})
export class AdminSupportDetail {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  readonly admin = inject(AdminService);
  readonly support = inject(SupportService);

  readonly ticket = this.support.currentTicket;
  readonly comments = this.support.comments;
  readonly attachments = this.support.attachments;
  readonly loading = this.support.loading;
  readonly error = this.support.error;
  readonly forbidden = signal(false);

  readonly replyText = signal('');
  readonly replyFile = signal<File | null>(null);
  readonly sending = signal(false);
  readonly updatingStatus = signal(false);
  readonly assigning = signal(false);

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

  readonly statusOptions = STATUS_OPTIONS;
  readonly statusLabel = (s: TicketStatus) => STATUS_LABELS[s] ?? s;
  readonly statusSeverity = (s: TicketStatus) => STATUS_SEVERITIES[s] ?? 'info';
  readonly priorityLabel = (p: TicketPriority) => PRIORITY_LABELS[p] ?? p;

  readonly clientLabel = (userId: number) => {
    const user = this.admin.users().find((u) => u.id === userId);
    return user ? user.email : `#${userId}`;
  };

  readonly agentOptions = computed(() =>
    this.admin.ticketAgents().map((a) => {
      const user = this.admin.users().find((u) => u.id === a.userId);
      return { label: user?.email ?? `#${a.userId}`, value: a.userId };
    }),
  );

  private readonly ticketId = Number(this.route.snapshot.paramMap.get('id'));

  constructor() {
    if (this.admin.ticketAgents().length === 0) this.admin.loadTicketAgents();
    if (this.admin.users().length === 0) this.admin.loadUsers();
    this.load();
  }

  async setStatus(status: TicketStatus): Promise<void> {
    this.updatingStatus.set(true);
    try {
      await this.support.updateStatus(this.ticketId, status);
    } finally {
      this.updatingStatus.set(false);
    }
  }

  async assign(agentId: number): Promise<void> {
    this.assigning.set(true);
    try {
      await this.admin.assignTicket(this.ticketId, agentId);
      await this.support.getTicket(this.ticketId);
    } finally {
      this.assigning.set(false);
    }
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

  private async load(): Promise<void> {
    try {
      await this.support.getTicket(this.ticketId);
      await this.support.getComments(this.ticketId);
      await this.support.getAttachments(this.ticketId);
    } catch (err: any) {
      if (err?.status === 403) {
        this.forbidden.set(true);
      } else {
        this.router.navigate(['/admin/support']);
      }
    }
  }
}
