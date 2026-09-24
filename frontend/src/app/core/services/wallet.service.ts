import { Injectable, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { firstValueFrom } from 'rxjs';
import { AdminAdjustRequest, PricingConfig, Wallet, WalletTransaction } from '../models/billing.model';

@Injectable({ providedIn: 'root' })
export class WalletService {
  private readonly http = inject(HttpClient);

  private readonly _wallet = signal<Wallet | null>(null);
  private readonly _transactions = signal<WalletTransaction[]>([]);
  private readonly _loading = signal(false);
  private readonly _error = signal<string | null>(null);

  readonly wallet = this._wallet.asReadonly();
  readonly transactions = this._transactions.asReadonly();
  readonly loading = this._loading.asReadonly();
  readonly error = this._error.asReadonly();

  async fetchWallet(userId: number): Promise<void> {
    this._loading.set(true);
    this._error.set(null);
    try {
      const wallet = await firstValueFrom(this.http.get<Wallet>(`/api/billing/wallet/user/${userId}`));
      this._wallet.set(wallet);
    } catch (err: any) {
      this._error.set(err?.error?.message || 'Erreur lors du chargement du solde');
    } finally {
      this._loading.set(false);
    }
  }

  async fetchTransactions(userId: number): Promise<void> {
    try {
      const transactions = await firstValueFrom(
        this.http.get<WalletTransaction[]>(`/api/billing/wallet/user/${userId}/transactions`),
      );
      this._transactions.set(Array.isArray(transactions) ? transactions : []);
    } catch (err: any) {
      this._error.set(err?.error?.message || 'Erreur lors du chargement des transactions');
    }
  }

  async recharge(userId: number, amount: number): Promise<void> {
    this._loading.set(true);
    this._error.set(null);
    try {
      const wallet = await firstValueFrom(
        this.http.post<Wallet>(`/api/billing/wallet/user/${userId}/recharge`, { amount }),
      );
      this._wallet.set(wallet);
      await this.fetchTransactions(userId);
    } catch (err: any) {
      this._error.set(err?.error?.message || 'Erreur lors de la recharge');
      throw err;
    } finally {
      this._loading.set(false);
    }
  }

  clearError(): void {
    this._error.set(null);
  }

  // ===== Admin =====

  async fetchAllWallets(): Promise<Wallet[]> {
    return firstValueFrom(this.http.get<Wallet[]>('/api/billing/admin/wallets'));
  }

  async adminAdjust(userId: number, request: AdminAdjustRequest): Promise<Wallet> {
    return firstValueFrom(this.http.post<Wallet>(`/api/billing/admin/wallet/user/${userId}/adjust`, request));
  }

  async fetchPricing(): Promise<PricingConfig> {
    return firstValueFrom(this.http.get<PricingConfig>('/api/billing/admin/pricing'));
  }

  async updatePricing(config: Partial<PricingConfig>): Promise<PricingConfig> {
    return firstValueFrom(this.http.put<PricingConfig>('/api/billing/admin/pricing', config));
  }
}
