import { Component, computed, effect, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ButtonModule } from 'primeng/button';
import { TableModule } from 'primeng/table';
import { TagModule } from 'primeng/tag';
import { InputNumberModule } from 'primeng/inputnumber';
import { AuthService } from '../../core/services/auth.service';
import { WalletService } from '../../core/services/wallet.service';
import { MonthlyInvoiceSummary, UsageSummary, WalletTransactionType, invoiceMonthLabel } from '../../core/models/billing.model';

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

const QUICK_AMOUNTS = [10, 25, 50, 100, 250];

// Same default as billing-service, used until the admin-set threshold has loaded.
const DEFAULT_LOW_BALANCE_THRESHOLD = 5;

/** Client "Paiements" page: balance and burn rate, add funds, invoices, transactions. */
@Component({
  selector: 'app-billing',
  imports: [CommonModule, FormsModule, ButtonModule, TableModule, TagModule, InputNumberModule],
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

  readonly quickAmounts = QUICK_AMOUNTS;
  /** Selected quick amount, or null when "Autre" is used. */
  readonly selectedAmount = signal<number | null>(25);
  readonly otherAmount = signal<number | null>(null);
  readonly addAmount = computed(() => this.selectedAmount() ?? this.otherAmount() ?? 0);
  readonly recharging = signal(false);
  readonly rechargeDone = signal(false);

  readonly usage = signal<UsageSummary | null>(null);
  readonly lowBalanceThreshold = signal(DEFAULT_LOW_BALANCE_THRESHOLD);
  readonly invoices = signal<MonthlyInvoiceSummary[]>([]);
  readonly invoicesLoading = signal(false);
  readonly invoicesError = signal<string | null>(null);
  readonly downloading = signal<string | null>(null);

  readonly currency = computed(() => this.wallet()?.currency || 'TND');
  readonly balance = computed(() => this.wallet()?.balance ?? 0);
  readonly newBalance = computed(() => this.balance() + this.addAmount());
  readonly creditExhausted = computed(() => this.wallet() !== null && this.balance() <= 0);
  readonly lowBalance = computed(() => this.wallet() !== null && this.balance() < this.lowBalanceThreshold());

  /** "3 j 4 h" style runway, or "—" when nothing is drawing the balance down. */
  readonly runwayLabel = computed(() => {
    const h = this.usage()?.runwayHours;
    if (h === null || h === undefined) return '—';
    const days = Math.floor(h / 24);
    const hours = Math.floor(h % 24);
    return days > 0 ? `${days} j ${hours} h` : `${hours} h`;
  });

  readonly typeLabel = (type: WalletTransactionType) => TYPE_LABELS[type] ?? type;
  readonly typeSeverity = (type: WalletTransactionType) => TYPE_SEVERITIES[type] ?? 'info';
  readonly monthLabel = invoiceMonthLabel;

  constructor() {
    effect(() => {
      const user = this.auth.user();
      if (user) this.loadAll(user.id);
    });
    this.walletService
      .fetchAlertThreshold()
      .then((t) => this.lowBalanceThreshold.set(t.threshold))
      .catch(() => {});
  }

  refresh(): void {
    const user = this.user();
    if (user) this.loadAll(user.id);
  }

  selectQuick(amount: number): void {
    this.selectedAmount.set(amount);
    this.otherAmount.set(null);
  }

  onOtherAmount(value: number | null): void {
    this.otherAmount.set(value);
    this.selectedAmount.set(null);
  }

  async addFunds(): Promise<void> {
    const user = this.user();
    const amount = this.addAmount();
    if (!user || amount <= 0) return;
    this.recharging.set(true);
    this.rechargeDone.set(false);
    try {
      await this.walletService.recharge(user.id, amount);
      this.rechargeDone.set(true);
      this.loadUsage(user.id);
      this.loadInvoices(user.id);
    } catch {
      // WalletService already exposes the error message through error().
    } finally {
      this.recharging.set(false);
    }
  }

  clearError(): void {
    this.walletService.clearError();
  }

  private loadAll(userId: number): void {
    this.walletService.fetchWallet(userId);
    this.walletService.fetchTransactions(userId);
    this.loadUsage(userId);
    this.loadInvoices(userId);
  }

  private async loadUsage(userId: number): Promise<void> {
    try {
      this.usage.set(await this.walletService.fetchUsage(userId, 30));
    } catch {
      this.usage.set(null);
    }
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
