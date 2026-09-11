import { Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { ButtonModule } from 'primeng/button';
import { TagModule } from 'primeng/tag';
import { SelectModule } from 'primeng/select';
import { AdminService } from '../../../../core/services/admin.service';
import { TicketCategory, TicketPriority, TicketStatus } from '../../../../core/models/support.model';

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

const STATUS_OPTIONS: { label: string; value: TicketStatus | null }[] = [
  { label: 'Tous les statuts', value: null },
  ...(Object.keys(STATUS_LABELS) as TicketStatus[]).map((value) => ({ label: STATUS_LABELS[value], value })),
];

const PRIORITY_OPTIONS: { label: string; value: TicketPriority | null }[] = [
  { label: 'Toutes les priorités', value: null },
  ...(Object.keys(PRIORITY_LABELS) as TicketPriority[]).map((value) => ({ label: PRIORITY_LABELS[value], value })),
];

const CATEGORY_LABELS: Record<TicketCategory, string> = {
  BILLING: 'Facturation',
  TECHNICAL: 'Technique',
  VPS: 'VPS',
  DOMAIN: 'Domaine',
  KUBERNETES: 'Kubernetes',
  ACCOUNT: 'Compte',
  OTHER: 'Autre',
};

const CATEGORY_OPTIONS: { label: string; value: TicketCategory | null }[] = [
  { label: 'Toutes les catégories', value: null },
  ...(Object.keys(CATEGORY_LABELS) as TicketCategory[]).map((value) => ({ label: CATEGORY_LABELS[value], value })),
];

@Component({
  selector: 'app-admin-support-list',
  imports: [CommonModule, RouterLink, FormsModule, ButtonModule, TagModule, SelectModule],
  templateUrl: './admin-support-list.html',
  styleUrl: './admin-support-list.scss',
})
export class AdminSupportList {
  private readonly router = inject(Router);
  readonly admin = inject(AdminService);

  readonly tickets = this.admin.allTickets;
  readonly loading = signal(true);
  readonly error = this.admin.error;
  readonly forbidden = signal(false);

  readonly statusOptions = STATUS_OPTIONS;
  readonly priorityOptions = PRIORITY_OPTIONS;
  readonly categoryOptions = CATEGORY_OPTIONS;

  readonly filterStatus = signal<TicketStatus | null>(null);
  readonly filterPriority = signal<TicketPriority | null>(null);
  readonly filterCategory = signal<TicketCategory | null>(null);

  readonly statusLabel = (s: TicketStatus) => STATUS_LABELS[s] ?? s;
  readonly statusSeverity = (s: TicketStatus) => STATUS_SEVERITIES[s] ?? 'info';
  readonly priorityLabel = (p: TicketPriority) => PRIORITY_LABELS[p] ?? p;

  readonly clientLabel = (userId: number) => {
    const user = this.admin.users().find((u) => u.id === userId);
    return user ? user.email : `#${userId}`;
  };

  constructor() {
    if (this.admin.users().length === 0) this.admin.loadUsers();
    this.load();
  }

  async applyFilters(): Promise<void> {
    await this.load();
  }

  openTicket(id: number): void {
    this.router.navigate(['/admin/support', id]);
  }

  private async load(): Promise<void> {
    this.loading.set(true);
    this.forbidden.set(false);
    try {
      await this.admin.loadAllTickets({
        status: this.filterStatus() ?? undefined,
        priority: this.filterPriority() ?? undefined,
        category: this.filterCategory() ?? undefined,
      });
    } catch (err: any) {
      if (err?.status === 403) {
        this.forbidden.set(true);
      }
    } finally {
      this.loading.set(false);
    }
  }
}
