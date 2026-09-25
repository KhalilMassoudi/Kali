import { Component, computed, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import { ButtonModule } from 'primeng/button';
import { TableModule } from 'primeng/table';
import { InputNumberModule } from 'primeng/inputnumber';
import { MessageModule } from 'primeng/message';
import { ToggleSwitchModule } from 'primeng/toggleswitch';
import { DialogModule } from 'primeng/dialog';
import { TagModule } from 'primeng/tag';
import { AdminService } from '../../../core/services/admin.service';
import { WalletService } from '../../../core/services/wallet.service';
import { MonthlyInvoiceSummary, Wallet, invoiceMonthLabel } from '../../../core/models/billing.model';

interface AdjustRowState {
  open: boolean;
  amount: number | null;
  saving: boolean;
}

@Component({
  selector: 'app-admin-billing',
  imports: [
    CommonModule,
    ReactiveFormsModule,
    FormsModule,
    ButtonModule,
    TableModule,
    InputNumberModule,
    MessageModule,
    ToggleSwitchModule,
    DialogModule,
    TagModule,
  ],
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
    lowBalanceThreshold: [5, [Validators.required, Validators.min(0)]],
    suspendVmsOnZeroBalance: [false],
  });

  // Last saved threshold - drives the "low" highlight in the balances table.
  readonly lowBalanceThreshold = signal(5);

  // Per-client invoices dialog
  readonly invoicesClient = signal<{ userId: number; label: string } | null>(null);
  readonly clientInvoices = signal<MonthlyInvoiceSummary[]>([]);
  readonly invoicesLoading = signal(false);
  readonly invoicesError = signal('');
  readonly downloading = signal<string | null>(null);
  readonly monthLabel = invoiceMonthLabel;

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
      this.lowBalanceThreshold.set(pricing.lowBalanceThreshold);
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
      this.lowBalanceThreshold.set(updated.lowBalanceThreshold);
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

  async openInvoices(wallet: Wallet, label: string): Promise<void> {
    this.invoicesClient.set({ userId: wallet.userId, label });
    this.clientInvoices.set([]);
    this.invoicesError.set('');
    this.invoicesLoading.set(true);
    try {
      this.clientInvoices.set(await this.walletService.fetchMonthlyInvoices(wallet.userId));
    } catch (err: any) {
      this.invoicesError.set(err?.error?.message || 'Erreur lors du chargement des factures');
    } finally {
      this.invoicesLoading.set(false);
    }
  }

  closeInvoices(): void {
    this.invoicesClient.set(null);
  }

  async downloadInvoice(invoice: MonthlyInvoiceSummary): Promise<void> {
    const client = this.invoicesClient();
    if (!client) return;
    this.downloading.set(invoice.month);
    this.invoicesError.set('');
    try {
      await this.walletService.downloadMonthlyInvoice(client.userId, invoice);
    } catch {
      this.invoicesError.set('Erreur lors du téléchargement de la facture');
    } finally {
      this.downloading.set(null);
    }
  }
}
