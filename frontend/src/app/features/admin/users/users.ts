import { Component, computed, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { ButtonModule } from 'primeng/button';
import { TableModule } from 'primeng/table';
import { TagModule } from 'primeng/tag';
import { TooltipModule } from 'primeng/tooltip';
import { InputTextModule } from 'primeng/inputtext';
import { SelectModule } from 'primeng/select';
import { AuthService } from '../../../core/services/auth.service';
import { AdminService } from '../../../core/services/admin.service';
import { WalletService } from '../../../core/services/wallet.service';
import { AdminUser, ApiLogEntry } from '../../../core/models/admin.model';
import { AdminClientSummary } from '../../../core/models/project.model';
import { Wallet, WalletTransaction } from '../../../core/models/billing.model';
import { EditQuotaDialog } from './edit-quota-dialog/edit-quota-dialog';

interface RowState {
  confirmDelete: boolean;
  actionLoading: 'role' | 'enabled' | 'delete' | null;
}

interface ActivityState {
  loaded: boolean;
  loading: boolean;
  entries: ApiLogEntry[];
  transactions: WalletTransaction[];
}

type RoleFilter = 'ALL' | 'ADMIN' | 'USER';
type StatusFilter = 'ALL' | 'ENABLED' | 'DISABLED';

const ROLE_FILTER_OPTIONS: { label: string; value: RoleFilter }[] = [
  { label: 'Tous les rôles', value: 'ALL' },
  { label: 'Admin', value: 'ADMIN' },
  { label: 'Client', value: 'USER' },
];

const STATUS_FILTER_OPTIONS: { label: string; value: StatusFilter }[] = [
  { label: 'Tous les statuts', value: 'ALL' },
  { label: 'Actif', value: 'ENABLED' },
  { label: 'Désactivé', value: 'DISABLED' },
];

@Component({
  selector: 'app-admin-users',
  imports: [
    CommonModule,
    FormsModule,
    ButtonModule,
    TableModule,
    TagModule,
    TooltipModule,
    InputTextModule,
    SelectModule,
    EditQuotaDialog,
  ],
  templateUrl: './users.html',
  styleUrl: './users.scss',
})
export class AdminUsers {
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  readonly admin = inject(AdminService);
  readonly walletService = inject(WalletService);

  readonly currentUserId = computed(() => this.auth.user()?.id ?? null);
  readonly users = this.admin.users;
  readonly loading = signal(true);
  readonly error = this.admin.error;

  readonly wallets = signal<Wallet[]>([]);

  readonly searchQuery = signal('');
  readonly roleFilter = signal<RoleFilter>('ALL');
  readonly statusFilter = signal<StatusFilter>('ALL');
  readonly roleFilterOptions = ROLE_FILTER_OPTIONS;
  readonly statusFilterOptions = STATUS_FILTER_OPTIONS;

  readonly filteredUsers = computed(() => {
    const query = this.searchQuery().trim().toLowerCase();
    const role = this.roleFilter();
    const status = this.statusFilter();
    return this.users().filter((u) => {
      if (query) {
        const haystack = `${u.email} ${u.firstName ?? ''} ${u.lastName ?? ''}`.toLowerCase();
        if (!haystack.includes(query)) return false;
      }
      if (role !== 'ALL' && u.role !== role) return false;
      if (status === 'ENABLED' && !u.enabled) return false;
      if (status === 'DISABLED' && u.enabled) return false;
      return true;
    });
  });

  private readonly rowStates = signal<Record<number, RowState>>({});
  private readonly activityStates = signal<Record<number, ActivityState>>({});
  readonly expandedRowKeys = signal<Record<string, boolean>>({});

  constructor() {
    this.load();
  }

  private async load(): Promise<void> {
    this.loading.set(true);
    try {
      const [wallets] = await Promise.all([
        this.walletService.fetchAllWallets(),
        this.admin.loadUsers(),
        this.admin.loadClientSummaries(),
        // Requires Ticket Agent status, separate from the ADMIN role - not every admin
        // has it, so a 403 here shouldn't break the rest of this page (wallets, search,
        // etc). Ticket counts just show as unavailable for that case.
        this.admin.loadAllTickets().catch(() => {
          this.admin.clearError();
        }),
      ]);
      this.wallets.set(wallets);
    } finally {
      this.loading.set(false);
    }
  }

  walletFor(user: AdminUser): Wallet | undefined {
    return this.wallets().find((w) => w.userId === user.id);
  }

  ticketCountFor(user: AdminUser): number {
    return this.admin.allTickets().filter((t) => t.userId === user.id).length;
  }

  openTicketCountFor(user: AdminUser): number {
    return this.admin
      .allTickets()
      .filter((t) => t.userId === user.id && t.status !== 'RESOLVED' && t.status !== 'CLOSED').length;
  }

  viewResources(user: AdminUser, page: 'compute' | 'volumes' | 'network'): void {
    this.router.navigate([`/admin/${page}`], { queryParams: { userId: user.id } });
  }

  viewBilling(): void {
    this.router.navigate(['/admin/billing']);
  }

  async onRowExpand(event: { data: AdminUser }): Promise<void> {
    const user = event.data;
    this.expandedRowKeys.update((keys) => ({ ...keys, [user.id]: true }));

    const current = this.activityStates()[user.id];
    if (current?.loaded) return;

    this.activityStates.update((s) => ({
      ...s,
      [user.id]: { loaded: false, loading: true, entries: [], transactions: [] },
    }));
    try {
      const [entries, transactions] = await Promise.all([
        this.admin.getRecentActivityForUser(user.email, 5),
        this.walletService.fetchTransactionsForAdmin(user.id),
      ]);
      this.activityStates.update((s) => ({
        ...s,
        [user.id]: { loaded: true, loading: false, entries, transactions: transactions.slice(0, 5) },
      }));
    } catch {
      this.activityStates.update((s) => ({
        ...s,
        [user.id]: { loaded: true, loading: false, entries: [], transactions: [] },
      }));
    }
  }

  onRowCollapse(event: { data: AdminUser }): void {
    this.expandedRowKeys.update((keys) => {
      const { [event.data.id]: _removed, ...rest } = keys;
      return rest;
    });
  }

  activityFor(user: AdminUser): ActivityState {
    return this.activityStates()[user.id] ?? { loaded: false, loading: false, entries: [], transactions: [] };
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
