import { Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ButtonModule } from 'primeng/button';
import { TableModule } from 'primeng/table';
import { TagModule } from 'primeng/tag';
import { AdminService } from '../../../core/services/admin.service';
import { DomainStatus } from '../../../core/models/admin.model';

const DOMAIN_LABELS: Record<DomainStatus, string> = {
  PENDING: 'En attente',
  ACTIVE: 'Actif',
  EXPIRED: 'Expiré',
  TRANSFERRED: 'Transféré',
  DELETED: 'Supprimé',
};

const DOMAIN_SEVERITIES: Record<DomainStatus, 'success' | 'warn' | 'info' | 'danger' | 'secondary'> = {
  PENDING: 'info',
  ACTIVE: 'success',
  EXPIRED: 'danger',
  TRANSFERRED: 'warn',
  DELETED: 'secondary',
};

@Component({
  selector: 'app-admin-domains',
  imports: [CommonModule, ButtonModule, TableModule, TagModule],
  templateUrl: './domains.html',
  styleUrl: './domains.scss',
})
export class AdminDomains {
  readonly admin = inject(AdminService);
  readonly loading = signal(true);
  readonly error = this.admin.error;
  readonly domains = this.admin.allDomains;

  readonly domainLabel = (status: DomainStatus) => DOMAIN_LABELS[status] ?? status;
  readonly domainSeverity = (status: DomainStatus) => DOMAIN_SEVERITIES[status] ?? 'info';

  constructor() {
    this.load();
  }

  async refresh(): Promise<void> {
    await this.load();
  }

  private async load(): Promise<void> {
    this.loading.set(true);
    try {
      await this.admin.loadAllDomains();
    } finally {
      this.loading.set(false);
    }
  }
}
