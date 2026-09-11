import { Component, EventEmitter, Output, effect, inject, input, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { DialogModule } from 'primeng/dialog';
import { InputNumberModule } from 'primeng/inputnumber';
import { ButtonModule } from 'primeng/button';
import { MessageModule } from 'primeng/message';
import { QuotaService } from '../../../../core/services/quota.service';
import { AdminUser } from '../../../../core/models/admin.model';
import { UserQuota } from '../../../../core/models/vm.model';

const DEFAULT_QUOTA: UserQuota = {
  maxVcpu: 8,
  maxRamMb: 16384,
  maxStorageGb: 200,
  maxVms: 5,
  maxVolumes: 5,
  maxNetworks: 3,
  maxSecurityGroups: 5,
};

@Component({
  selector: 'app-edit-quota-dialog',
  imports: [CommonModule, FormsModule, DialogModule, InputNumberModule, ButtonModule, MessageModule],
  templateUrl: './edit-quota-dialog.html',
  styleUrl: './edit-quota-dialog.scss',
})
export class EditQuotaDialog {
  private readonly quotaService = inject(QuotaService);

  readonly user = input<AdminUser | null>(null);
  @Output() readonly closed = new EventEmitter<void>();

  readonly visible = signal(false);
  readonly loading = signal(false);
  readonly saving = signal(false);
  readonly error = signal('');
  readonly quota = signal<UserQuota>(DEFAULT_QUOTA);

  constructor() {
    effect(() => {
      const u = this.user();
      if (u) this.load(u.id);
    });
  }

  private async load(userId: number): Promise<void> {
    this.visible.set(true);
    this.loading.set(true);
    this.error.set('');
    try {
      const result = await this.quotaService.getQuota(userId);
      this.quota.set(result.quota);
    } catch {
      this.error.set('Erreur lors du chargement du quota');
    } finally {
      this.loading.set(false);
    }
  }

  set<K extends keyof UserQuota>(key: K, value: number): void {
    this.quota.update((q) => ({ ...q, [key]: value }));
  }

  onHide(): void {
    this.visible.set(false);
    this.closed.emit();
  }

  async save(): Promise<void> {
    const u = this.user();
    if (!u) return;
    this.saving.set(true);
    this.error.set('');
    try {
      await this.quotaService.updateQuota(u.id, this.quota());
      this.onHide();
    } catch {
      this.error.set('Erreur lors de la mise à jour du quota');
    } finally {
      this.saving.set(false);
    }
  }
}
