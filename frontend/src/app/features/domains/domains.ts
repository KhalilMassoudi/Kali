import { Component, effect, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ButtonModule } from 'primeng/button';
import { TableModule } from 'primeng/table';
import { TagModule } from 'primeng/tag';
import { TooltipModule } from 'primeng/tooltip';
import { AuthService } from '../../core/services/auth.service';
import { DomainService } from '../../core/services/domain.service';
import { Domain, DomainStatus } from '../../core/models/vm.model';
import { CreateDomainDialog } from './create-domain-dialog/create-domain-dialog';

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
  selector: 'app-domains',
  imports: [CommonModule, ButtonModule, TableModule, TagModule, TooltipModule, CreateDomainDialog],
  templateUrl: './domains.html',
  styleUrl: './domains.scss',
})
export class Domains {
  private readonly auth = inject(AuthService);
  readonly domainService = inject(DomainService);

  readonly user = this.auth.user;
  readonly domains = this.domainService.domains;
  readonly loading = this.domainService.loading;
  readonly error = this.domainService.error;

  readonly showCreateDialog = signal(false);
  private readonly deletingIds = signal<Set<number>>(new Set());

  readonly domainLabel = (status: DomainStatus) => DOMAIN_LABELS[status] ?? status;
  readonly domainSeverity = (status: DomainStatus) => DOMAIN_SEVERITIES[status] ?? 'info';

  constructor() {
    effect(() => {
      const user = this.auth.user();
      if (user) this.domainService.fetchDomains(user.id);
    });
  }

  refresh(): void {
    const user = this.user();
    if (user) this.domainService.fetchDomains(user.id);
  }

  clearError(): void {
    this.domainService.clearError();
  }

  isDeleting(domain: Domain): boolean {
    return this.deletingIds().has(domain.id);
  }

  async remove(domain: Domain): Promise<void> {
    this.deletingIds.update((ids) => new Set(ids).add(domain.id));
    try {
      await this.domainService.deleteDomain(domain.id);
    } finally {
      this.deletingIds.update((ids) => {
        const next = new Set(ids);
        next.delete(domain.id);
        return next;
      });
    }
  }
}
