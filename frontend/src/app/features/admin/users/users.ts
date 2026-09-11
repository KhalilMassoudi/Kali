import { Component, computed, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ButtonModule } from 'primeng/button';
import { TableModule } from 'primeng/table';
import { TagModule } from 'primeng/tag';
import { TooltipModule } from 'primeng/tooltip';
import { AuthService } from '../../../core/services/auth.service';
import { AdminService } from '../../../core/services/admin.service';
import { AdminUser } from '../../../core/models/admin.model';
import { AdminClientSummary } from '../../../core/models/project.model';
import { EditQuotaDialog } from './edit-quota-dialog/edit-quota-dialog';

interface RowState {
  confirmDelete: boolean;
  actionLoading: 'role' | 'enabled' | 'delete' | null;
}

@Component({
  selector: 'app-admin-users',
  imports: [CommonModule, ButtonModule, TableModule, TagModule, TooltipModule, EditQuotaDialog],
  templateUrl: './users.html',
  styleUrl: './users.scss',
})
export class AdminUsers {
  private readonly auth = inject(AuthService);
  readonly admin = inject(AdminService);

  readonly currentUserId = computed(() => this.auth.user()?.id ?? null);
  readonly users = this.admin.users;
  readonly loading = signal(true);
  readonly error = this.admin.error;

  private readonly rowStates = signal<Record<number, RowState>>({});
  readonly expandedRowKeys = signal<Record<string, boolean>>({});

  constructor() {
    this.load();
  }

  private async load(): Promise<void> {
    this.loading.set(true);
    try {
      await Promise.all([this.admin.loadUsers(), this.admin.loadClientSummaries()]);
    } finally {
      this.loading.set(false);
    }
  }

  onRowExpand(event: { data: AdminUser }): void {
    this.expandedRowKeys.update((keys) => ({ ...keys, [event.data.id]: true }));
  }

  onRowCollapse(event: { data: AdminUser }): void {
    this.expandedRowKeys.update((keys) => {
      const { [event.data.id]: _removed, ...rest } = keys;
      return rest;
    });
  }

  clientSummaryFor(user: AdminUser): AdminClientSummary | undefined {
    return this.admin.clientSummaries().find((s) => s.userId === user.id);
  }

  rowState(user: AdminUser): RowState {
    return this.rowStates()[user.id] ?? { confirmDelete: false, actionLoading: null };
  }

  private patchRow(id: number, patch: Partial<RowState>): void {
    this.rowStates.update((states) => ({
      ...states,
      [id]: { ...(states[id] ?? { confirmDelete: false, actionLoading: null }), ...patch },
    }));
  }

  readonly quotaTarget = signal<AdminUser | null>(null);

  isSelf(user: AdminUser): boolean {
    return user.id === this.currentUserId();
  }

  manageQuota(user: AdminUser): void {
    this.quotaTarget.set(user);
  }

  async toggleRole(user: AdminUser): Promise<void> {
    this.patchRow(user.id, { actionLoading: 'role' });
    try {
      await this.admin.updateUserRole(user.id, user.role === 'ADMIN' ? 'USER' : 'ADMIN');
    } finally {
      this.patchRow(user.id, { actionLoading: null });
    }
  }

  async toggleEnabled(user: AdminUser): Promise<void> {
    this.patchRow(user.id, { actionLoading: 'enabled' });
    try {
      await this.admin.setUserEnabled(user.id, !user.enabled);
    } finally {
      this.patchRow(user.id, { actionLoading: null });
    }
  }

  askDelete(user: AdminUser): void {
    this.patchRow(user.id, { confirmDelete: true });
  }

  cancelDelete(user: AdminUser): void {
    this.patchRow(user.id, { confirmDelete: false });
  }

  async confirmDelete(user: AdminUser): Promise<void> {
    this.patchRow(user.id, { actionLoading: 'delete' });
    try {
      await this.admin.deleteUser(user.id);
    } finally {
      this.patchRow(user.id, { actionLoading: null, confirmDelete: false });
    }
  }

  clearError(): void {
    this.admin.clearError();
  }
}