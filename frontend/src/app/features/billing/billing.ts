import { Component, effect, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ButtonModule } from 'primeng/button';
import { TableModule } from 'primeng/table';
import { TagModule } from 'primeng/tag';
import { AuthService } from '../../core/services/auth.service';
import { WalletService } from '../../core/services/wallet.service';
import { WalletTransactionType } from '../../core/models/billing.model';
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

  readonly typeLabel = (type: WalletTransactionType) => TYPE_LABELS[type] ?? type;
  readonly typeSeverity = (type: WalletTransactionType) => TYPE_SEVERITIES[type] ?? 'info';

  constructor() {
    effect(() => {
      const user = this.auth.user();
      if (user) {
        this.walletService.fetchWallet(user.id);
        this.walletService.fetchTransactions(user.id);
      }
    });
  }

  refresh(): void {
    const user = this.user();
    if (user) {
      this.walletService.fetchWallet(user.id);
      this.walletService.fetchTransactions(user.id);
    }
  }

  clearError(): void {
    this.walletService.clearError();
  }
}
