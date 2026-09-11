import { Component, computed, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ButtonModule } from 'primeng/button';
import { SelectModule } from 'primeng/select';
import { AdminService } from '../../../../core/services/admin.service';

@Component({
  selector: 'app-admin-support-agents',
  imports: [CommonModule, FormsModule, ButtonModule, SelectModule],
  templateUrl: './admin-support-agents.html',
  styleUrl: './admin-support-agents.scss',
})
export class AdminSupportAgents {
  readonly admin = inject(AdminService);

  readonly loading = signal(true);
  readonly error = this.admin.error;
  readonly agents = this.admin.ticketAgents;

  readonly selectedUserId = signal<number | null>(null);
  readonly granting = signal(false);
  readonly actionLoading = signal<Record<number, boolean>>({});

  readonly agentRows = computed(() =>
    this.agents().map((a) => {
      const user = this.admin.users().find((u) => u.id === a.userId);
      return { ...a, email: user?.email ?? `#${a.userId}`, name: user ? `${user.firstName ?? ''} ${user.lastName ?? ''}`.trim() : '' };
    }),
  );

  readonly grantableAdmins = computed(() => {
    const grantedIds = new Set(this.agents().map((a) => a.userId));
    return this.admin
      .users()
      .filter((u) => u.role === 'ADMIN' && !grantedIds.has(u.id))
      .map((u) => ({ label: `${u.email}${u.firstName ? ' — ' + u.firstName : ''}`, value: u.id }));
  });

  constructor() {
    this.load();
  }

  async refresh(): Promise<void> {
    await this.load();
  }

  async grant(): Promise<void> {
    const userId = this.selectedUserId();
    if (!userId) return;
    this.granting.set(true);
    try {
      await this.admin.grantTicketAgent(userId);
      this.selectedUserId.set(null);
    } finally {
      this.granting.set(false);
    }
  }

  async revoke(userId: number): Promise<void> {
    this.actionLoading.update((m) => ({ ...m, [userId]: true }));
    try {
      await this.admin.revokeTicketAgent(userId);
    } finally {
      this.actionLoading.update((m) => ({ ...m, [userId]: false }));
    }
  }

  private async load(): Promise<void> {
    this.loading.set(true);
    try {
      await Promise.all([this.admin.loadTicketAgents(), this.admin.loadUsers()]);
    } finally {
      this.loading.set(false);
    }
  }
}
