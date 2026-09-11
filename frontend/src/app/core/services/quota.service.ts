import { Injectable, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { firstValueFrom } from 'rxjs';
import { QuotaResponse, UserQuota } from '../models/vm.model';

@Injectable({ providedIn: 'root' })
export class QuotaService {
  private readonly http = inject(HttpClient);

  private readonly _myQuota = signal<QuotaResponse | null>(null);
  private readonly _error = signal<string | null>(null);

  readonly myQuota = this._myQuota.asReadonly();
  readonly error = this._error.asReadonly();

  async loadMyQuota(): Promise<void> {
    try {
      const result = await firstValueFrom(this.http.get<QuotaResponse>('/api/infrastructure/quota/me'));
      this._myQuota.set(result);
    } catch (err: any) {
      this._error.set(err?.error?.message || 'Erreur lors du chargement du quota');
    }
  }

  async getQuota(userId: number): Promise<QuotaResponse> {
    return firstValueFrom(this.http.get<QuotaResponse>(`/api/infrastructure/admin/quotas/${userId}`));
  }

  async updateQuota(userId: number, data: UserQuota): Promise<UserQuota> {
    return firstValueFrom(this.http.put<UserQuota>(`/api/infrastructure/admin/quotas/${userId}`, data));
  }
}
