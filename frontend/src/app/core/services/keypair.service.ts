import { Injectable, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { firstValueFrom } from 'rxjs';
import { ClientKeypair, CreateKeypairRequest, CreateKeypairResponse } from '../models/vm.model';

@Injectable({ providedIn: 'root' })
export class KeypairService {
  private readonly http = inject(HttpClient);

  private readonly _keypairs = signal<ClientKeypair[]>([]);
  private readonly _loading = signal(false);
  private readonly _error = signal<string | null>(null);

  readonly keypairs = this._keypairs.asReadonly();
  readonly loading = this._loading.asReadonly();
  readonly error = this._error.asReadonly();

  async fetchKeypairs(userId: number): Promise<void> {
    this._loading.set(true);
    this._error.set(null);
    try {
      const keypairs = await firstValueFrom(this.http.get<ClientKeypair[]>(`/api/infrastructure/keypairs/user/${userId}`));
      this._keypairs.set(Array.isArray(keypairs) ? keypairs : []);
    } catch (err: any) {
      this._error.set(err?.error?.message || 'Erreur lors du chargement des paires de clés');
    } finally {
      this._loading.set(false);
    }
  }

  async createKeypair(data: CreateKeypairRequest): Promise<CreateKeypairResponse> {
    this._loading.set(true);
    this._error.set(null);
    try {
      const result = await firstValueFrom(this.http.post<CreateKeypairResponse>('/api/infrastructure/keypairs', data));
      this._keypairs.update((keypairs) => [...keypairs, result.keypair]);
      return result;
    } catch (err: any) {
      this._error.set(err?.error?.message || 'Erreur lors de la création de la paire de clés');
      throw err;
    } finally {
      this._loading.set(false);
    }
  }

  async deleteKeypair(id: number): Promise<void> {
    try {
      await firstValueFrom(this.http.delete(`/api/infrastructure/keypairs/${id}`));
      this._keypairs.update((keypairs) => keypairs.filter((k) => k.id !== id));
    } catch (err: any) {
      this._error.set(err?.error?.message || 'Erreur lors de la suppression de la paire de clés');
      throw err;
    }
  }

  clearError(): void {
    this._error.set(null);
  }
}
