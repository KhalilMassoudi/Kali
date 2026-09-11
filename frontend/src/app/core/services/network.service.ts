import { Injectable, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { firstValueFrom } from 'rxjs';
import { CreateNetworkRequest, VpsNetwork } from '../models/vm.model';

@Injectable({ providedIn: 'root' })
export class NetworkService {
  private readonly http = inject(HttpClient);

  private readonly _networks = signal<VpsNetwork[]>([]);
  private readonly _loading = signal(false);
  private readonly _error = signal<string | null>(null);

  readonly networks = this._networks.asReadonly();
  readonly loading = this._loading.asReadonly();
  readonly error = this._error.asReadonly();

  async fetchNetworks(userId: number): Promise<void> {
    this._loading.set(true);
    this._error.set(null);
    try {
      const networks = await firstValueFrom(this.http.get<VpsNetwork[]>(`/api/infrastructure/networks/user/${userId}`));
      this._networks.set(Array.isArray(networks) ? networks : []);
    } catch (err: any) {
      this._error.set(err?.error?.message || 'Erreur lors du chargement des réseaux');
    } finally {
      this._loading.set(false);
    }
  }

  async createNetwork(data: CreateNetworkRequest): Promise<VpsNetwork> {
    this._loading.set(true);
    this._error.set(null);
    try {
      const network = await firstValueFrom(this.http.post<VpsNetwork>('/api/infrastructure/networks', data));
      this._networks.update((networks) => [...networks, network]);
      return network;
    } catch (err: any) {
      this._error.set(err?.error?.message || 'Erreur lors de la création du réseau');
      throw err;
    } finally {
      this._loading.set(false);
    }
  }

  async assignProject(id: number, projectId: number | null): Promise<void> {
    try {
      const updated = await firstValueFrom(this.http.put<VpsNetwork>(`/api/infrastructure/networks/${id}/project`, { projectId }));
      this._networks.update((networks) => networks.map((n) => (n.id === id ? updated : n)));
    } catch (err: any) {
      this._error.set(err?.error?.message || "Erreur lors de l'assignation du projet");
      throw err;
    }
  }

  async deleteNetwork(id: number): Promise<void> {
    try {
      await firstValueFrom(this.http.delete(`/api/infrastructure/networks/${id}`));
      this._networks.update((networks) => networks.filter((n) => n.id !== id));
    } catch (err: any) {
      this._error.set(err?.error?.message || 'Erreur lors de la suppression du réseau');
      throw err;
    }
  }

  clearError(): void {
    this._error.set(null);
  }
}
