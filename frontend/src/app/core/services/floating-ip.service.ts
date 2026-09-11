import { Injectable, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { firstValueFrom } from 'rxjs';
import { AllocateFloatingIpRequest, ClientFloatingIp } from '../models/vm.model';

@Injectable({ providedIn: 'root' })
export class FloatingIpService {
  private readonly http = inject(HttpClient);

  private readonly _floatingIps = signal<ClientFloatingIp[]>([]);
  private readonly _loading = signal(false);
  private readonly _error = signal<string | null>(null);

  readonly floatingIps = this._floatingIps.asReadonly();
  readonly loading = this._loading.asReadonly();
  readonly error = this._error.asReadonly();

  async fetchPools(): Promise<string[]> {
    try {
      return await firstValueFrom(this.http.get<string[]>('/api/infrastructure/catalog/floating-ip-pools'));
    } catch {
      return [];
    }
  }

  async fetchFloatingIps(userId: number): Promise<void> {
    this._loading.set(true);
    this._error.set(null);
    try {
      const ips = await firstValueFrom(this.http.get<ClientFloatingIp[]>(`/api/infrastructure/floating-ips/user/${userId}`));
      this._floatingIps.set(Array.isArray(ips) ? ips : []);
    } catch (err: any) {
      this._error.set(err?.error?.message || 'Erreur lors du chargement des IP flottantes');
    } finally {
      this._loading.set(false);
    }
  }

  async allocate(data: AllocateFloatingIpRequest): Promise<ClientFloatingIp> {
    this._loading.set(true);
    this._error.set(null);
    try {
      const ip = await firstValueFrom(this.http.post<ClientFloatingIp>('/api/infrastructure/floating-ips', data));
      this._floatingIps.update((ips) => [...ips, ip]);
      return ip;
    } catch (err: any) {
      this._error.set(err?.error?.message || "Erreur lors de l'allocation de l'IP flottante");
      throw err;
    } finally {
      this._loading.set(false);
    }
  }

  async associate(id: number, vpsId: number): Promise<void> {
    try {
      const updated = await firstValueFrom(
        this.http.post<ClientFloatingIp>(`/api/infrastructure/floating-ips/${id}/associate`, { vpsId }),
      );
      this._floatingIps.update((ips) => ips.map((ip) => (ip.id === id ? updated : ip)));
    } catch (err: any) {
      this._error.set(err?.error?.message || "Erreur lors de l'association de l'IP flottante");
      throw err;
    }
  }

  async disassociate(id: number): Promise<void> {
    try {
      const updated = await firstValueFrom(
        this.http.post<ClientFloatingIp>(`/api/infrastructure/floating-ips/${id}/disassociate`, {}),
      );
      this._floatingIps.update((ips) => ips.map((ip) => (ip.id === id ? updated : ip)));
    } catch (err: any) {
      this._error.set(err?.error?.message || "Erreur lors de la dissociation de l'IP flottante");
      throw err;
    }
  }

  async release(id: number): Promise<void> {
    try {
      await firstValueFrom(this.http.delete(`/api/infrastructure/floating-ips/${id}`));
      this._floatingIps.update((ips) => ips.filter((ip) => ip.id !== id));
    } catch (err: any) {
      this._error.set(err?.error?.message || "Erreur lors de la libération de l'IP flottante");
      throw err;
    }
  }

  clearError(): void {
    this._error.set(null);
  }
}
