import { Component, computed, effect, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ButtonModule } from 'primeng/button';
import { TableModule } from 'primeng/table';
import { TagModule } from 'primeng/tag';
import { AuthService } from '../../core/services/auth.service';
import { WalletService } from '../../core/services/wallet.service';
import { MonthlyInvoiceSummary, WalletTransactionType, invoiceMonthLabel } from '../../core/models/billing.model';
import { RechargeDialog } from './recharge-dialog/recharge-dialog';

const TYPE_LABELS: Record<WalletTransactionType, string> = {
  RECHARGE: 'Recharge',
  USAGE_DEDUCTION: 'Consommation',
  ADMIN_ADJUSTMENT: 'Ajustement admin',
};

const TYPE_SEVERITIES: Record<WalletTransactionType, 'success' | 'warn' | 'info' | 'danger' | 'secondary'> = {
  RECHARGE: 'success',
  USAGE_DEDUCTION: 'secondary',
  ADMIN_ADJUSTMENT: 'info',
};

// Same default as billing-service, used until the admin-set threshold has loaded.
const DEFAULT_LOW_BALANCE_THRESHOLD = 5;

@Component({
  selector: 'app-billing',
  imports: [CommonModule, ButtonModule, TableModule, TagModule, RechargeDialog],
  templateUrl: './billing.html',
  styleUrl: './billing.scss',
})
export class Billing {
  private readonly auth = inject(AuthService);
  readonly walletService = inject(WalletService);

  readonly user = this.auth.user;
  readonly wallet = this.walletService.wallet;
  readonly transactions = this.walletService.transactions;
  readonly loading = this.walletService.loading;
  readonly error = this.walletService.error;

  readonly showRechargeDialog = signal(false);

  readonly lowBalanceThreshold = signal(DEFAULT_LOW_BALANCE_THRESHOLD);
  readonly invoices = signal<MonthlyInvoiceSummary[]>([]);
  readonly invoicesLoading = signal(false);
  readonly invoicesError = signal<string | null>(null);
  readonly downloading = signal<string | null>(null);

  readonly balance = computed(() => this.wallet()?.balance ?? 0);
  readonly creditExhausted = computed(() => this.wallet() !== null && this.balance() <= 0);
  readonly lowBalance = computed(() => this.wallet() !== null && this.balance() < this.lowBalanceThreshold());

  readonly typeLabel = (type: WalletTransactionType) => TYPE_LABELS[type] ?? type;
  readonly typeSeverity = (type: WalletTransactionType) => TYPE_SEVERITIES[type] ?? 'info';
  readonly monthLabel = invoiceMonthLabel;

  constructor() {
    effect(() => {
      const user = this.auth.user();
      if (user) {
        this.walletService.fetchWallet(user.id);
        this.walletService.fetchTransactions(user.id);
        this.loadInvoices(user.id);
      }
    });
    this.walletService
      .fetchAlertThreshold()
      .then((t) => this.lowBalanceThreshold.set(t.threshold))
      .catch(() => {});
  }

  refresh(): void {
    const user = this.user();
    if (user) {
      this.walletService.fetchWallet(user.id);
      this.walletService.fetchTransactions(user.id);
      this.loadInvoices(user.id);
    }
  }

  clearError(): void {
    this.walletService.clearError();
  }

  private async loadInvoices(userId: number): Promise<void> {
    this.invoicesLoading.set(true);
    this.invoicesError.set(null);
    try {
      this.invoices.set(await this.walletService.fetchMonthlyInvoices(userId));
    } catch (err: any) {
      this.invoicesError.set(err?.error?.message || 'Erreur lors du chargement des factures');
    } finally {
      this.invoicesLoading.set(false);
    }
  }

  async downloadInvoice(invoice: MonthlyInvoiceSummary): Promise<void> {
    const user = this.user();
    if (!user) return;
    this.downloading.set(invoice.month);
    this.invoicesError.set(null);
    try {
      await this.walletService.downloadMonthlyInvoice(user.id, invoice);
    } catch {
      this.invoicesError.set('Erreur lors du téléchargement de la facture');
    } finally {
      this.downloading.set(null);
    }
  }
}
