import { Component, computed, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import { ButtonModule } from 'primeng/button';
import { TableModule } from 'primeng/table';
import { InputNumberModule } from 'primeng/inputnumber';
import { MessageModule } from 'primeng/message';
import { AdminService } from '../../../core/services/admin.service';
import { WalletService } from '../../../core/services/wallet.service';
import { PricingConfig, Wallet } from '../../../core/models/billing.model';

interface AdjustRowState {
  open: boolean;
  amount: number | null;
  saving: boolean;
}

@Component({
  selector: 'app-admin-billing',
  imports: [CommonModule, ReactiveFormsModule, FormsModule, ButtonModule, TableModule, InputNumberModule, MessageModule],
  templateUrl: './billing.html',
  styleUrl: './billing.scss',
})
export class AdminBilling {
  private readonly fb = inject(FormBuilder);
  readonly admin = inject(AdminService);
  readonly walletService = inject(WalletService);

  readonly users = this.admin.users;
  readonly wallets = signal<Wallet[]>([]);
  readonly loading = signal(true);
  readonly pricingError = signal('');
  readonly pricingSaved = signal(false);

  private readonly rowStates = signal<Record<number, AdjustRowState>>({});

  readonly pricingForm = this.fb.nonNullable.group({
    pricePerVcpuHour: [0, [Validators.required, Validators.min(0)]],
    pricePerRamGbHour: [0, [Validators.required, Validators.min(0)]],
    pricePerStorageGbHour: [0, [Validators.required, Validators.min(0)]],
    pricePerFloatingIpHour: [0, [Validators.required, Validators.min(0)]],
  });

  readonly walletRows = computed(() => {
    const usersById = new Map(this.users().map((u) => [u.id, u]));
    return this.wallets().map((w) => ({ wallet: w, user: usersById.get(w.userId) }));
  });

  constructor() {
    this.load();
  }

  async refresh(): Promise<void> {
    await this.load();
  }

  private async load(): Promise<void> {
    this.loading.set(true);
    try {
      const [wallets, pricing] = await Promise.all([
        this.walletService.fetchAllWallets(),
        this.walletService.fetchPricing(),
        this.admin.loadUsers(),
      ]);
      this.wallets.set(wallets);
      this.pricingForm.patchValue(pricing);
    } finally {
      this.loading.set(false);
    }
  }

  async savePricing(): Promise<void> {
    this.pricingError.set('');
    this.pricingSaved.set(false);
    if (this.pricingForm.invalid) {
      this.pricingForm.markAllAsTouched();
      return;
    }
    try {
      const updated = await this.walletService.updatePricing(this.pricingForm.getRawValue());
      this.pricingForm.patchValue(updated);
      this.pricingSaved.set(true);
    } catch (err: any) {
      this.pricingError.set(err?.error?.message || 'Erreur lors de la mise à jour de la tarification');
    }
  }

  rowState(wallet: Wallet): AdjustRowState {
    return this.rowStates()[wallet.id] ?? { open: false, amount: null, saving: false };
  }

  toggleAdjust(wallet: Wallet): void {
    const current = this.rowState(wallet);
    this.rowStates.update((s) => ({ ...s, [wallet.id]: { ...current, open: !current.open } }));
  }

  setAdjustAmount(wallet: Wallet, amount: number | null): void {
    const current = this.rowState(wallet);
    this.rowStates.update((s) => ({ ...s, [wallet.id]: { ...current, amount } }));
  }

  async applyAdjust(wallet: Wallet): Promise<void> {
    const current = this.rowState(wallet);
    if (!current.amount) return;
    this.rowStates.update((s) => ({ ...s, [wallet.id]: { ...current, saving: true } }));
    try {
      const updated = await this.walletService.adminAdjust(wallet.userId, {
        amount: current.amount,
        description: 'Ajustement manuel (admin)',
      });
      this.wallets.update((list) => list.map((w) => (w.id === updated.id ? updated : w)));
      this.rowStates.update((s) => ({ ...s, [wallet.id]: { open: false, amount: null, saving: false } }));
    } catch {
      this.rowStates.update((s) => ({ ...s, [wallet.id]: { ...current, saving: false } }));
    }
  }
}
