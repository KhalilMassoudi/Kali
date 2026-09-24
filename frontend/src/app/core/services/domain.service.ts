import { Injectable, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { firstValueFrom } from 'rxjs';
import { CreateDomainRequest, Domain } from '../models/vm.model';

@Injectable({ providedIn: 'root' })
export class DomainService {
  private readonly http = inject(HttpClient);

  private readonly _domains = signal<Domain[]>([]);
  private readonly _loading = signal(false);
  private readonly _error = signal<string | null>(null);

  readonly domains = this._domains.asReadonly();
  readonly loading = this._loading.asReadonly();
  readonly error = this._error.asReadonly();

  async fetchDomains(userId: number): Promise<void> {
    this._loading.set(true);
    this._error.set(null);
    try {
      const domains = await firstValueFrom(this.http.get<Domain[]>(`/api/infrastructure/domains/user/${userId}`));
      this._domains.set(Array.isArray(domains) ? domains : []);
    } catch (err: any) {
      this._error.set(err?.error?.message || 'Erreur lors du chargement des domaines');
    } finally {
      this._loading.set(false);
    }
  }

  async createDomain(data: CreateDomainRequest): Promise<Domain> {
    this._loading.set(true);
    this._error.set(null);
    try {
      const domain = await firstValueFrom(this.http.post<Domain>('/api/infrastructure/domains', data));
      this._domains.update((domains) => [...domains, domain]);
      return domain;
    } catch (err: any) {
      this._error.set(err?.error?.message || 'Erreur lors de la création du domaine');
      throw err;
    } finally {
      this._loading.set(false);
    }
  }

  async deleteDomain(id: number): Promise<void> {
    try {
      await firstValueFrom(this.http.delete(`/api/infrastructure/domains/${id}`));
      this._domains.update((domains) => domains.filter((d) => d.id !== id));
    } catch (err: any) {
      this._error.set(err?.error?.message || 'Erreur lors de la suppression du domaine');
      throw err;
    }
  }

  clearError(): void {
    this._error.set(null);
  }
}
