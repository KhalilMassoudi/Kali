import { Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterLink } from '@angular/router';
import { ButtonModule } from 'primeng/button';
import { TagModule } from 'primeng/tag';
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
  selector: 'app-support-list',
  imports: [CommonModule, RouterLink, ButtonModule, TagModule],
  templateUrl: './support-list.html',
  styleUrl: './support-list.scss',
})
export class SupportList {
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  readonly support = inject(SupportService);

  readonly tickets = this.support.tickets;
  readonly loading = this.support.loading;
  readonly error = this.support.error;

  readonly statusLabel = (s: TicketStatus) => STATUS_LABELS[s] ?? s;
  readonly statusSeverity = (s: TicketStatus) => STATUS_SEVERITIES[s] ?? 'info';
  readonly priorityLabel = (p: TicketPriority) => PRIORITY_LABELS[p] ?? p;

  constructor() {
    const userId = this.auth.user()?.id;
    if (userId) {
      this.support.fetchMyTickets(userId);
    }
  }

  openTicket(id: number): void {
    this.router.navigate(['/support', id]);
  }
}
