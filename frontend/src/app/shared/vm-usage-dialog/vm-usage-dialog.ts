import { Component, computed, effect, inject, input, model, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { DialogModule } from 'primeng/dialog';
import { AuthService } from '../../core/services/auth.service';
import { WalletService } from '../../core/services/wallet.service';
import { USAGE_TYPE_LABELS, VmUsage } from '../../core/models/billing.model';
import { Vm } from '../../core/models/vm.model';

/** "Suivi de consommation" of one VM: what it cost, tick by tick, and the wallet around each deduction. */
@Component({
  selector: 'app-vm-usage-dialog',
  imports: [CommonModule, DialogModule],
  templateUrl: './vm-usage-dialog.html',
  styleUrl: './vm-usage-dialog.scss',
})
export class VmUsageDialog {
  private readonly auth = inject(AuthService);
  private readonly walletService = inject(WalletService);

  readonly vm = input<Vm | null>(null);
  readonly visible = model<boolean>(false);

  readonly loading = signal(false);
  readonly error = signal<string | null>(null);
  readonly usage = signal<VmUsage | null>(null);

  readonly currency = computed(() => this.usage()?.currency ?? 'TND');
  readonly typeLabel = (t: string) => USAGE_TYPE_LABELS[t] ?? t;

  constructor() {
    effect(() => {
      if (this.visible() && this.vm()) this.load();
      else if (!this.visible()) {
        this.usage.set(null);
        this.error.set(null);
      }
    });
  }

  typesOf(byType: Record<string, number>): string {
    return Object.entries(byType)
      .map(([t, v]) => `${this.typeLabel(t)} ${v.toFixed(4)}`)
      .join(' · ');
  }

  private async load(): Promise<void> {
    const vm = this.vm();
    // The VM's owner, so an admin opening a client's VM sees that client's charges.
    const userId = vm?.userId ?? this.auth.user()?.id;
    if (!vm || !userId) return;
    this.loading.set(true);
    this.error.set(null);
    try {
      this.usage.set(await this.walletService.fetchVmUsage(userId, vm.id));
    } catch (err: any) {
      this.error.set(err?.error?.message || 'Erreur lors du chargement de la consommation');
    } finally {
      this.loading.set(false);
    }
  }
}
