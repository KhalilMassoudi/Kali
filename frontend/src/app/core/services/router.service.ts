import { Injectable, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { firstValueFrom } from 'rxjs';
import { ClientRouter, CreateRouterRequest, RouterInterfaceDetails } from '../models/vm.model';

@Injectable({ providedIn: 'root' })
export class RouterService {
  private readonly http = inject(HttpClient);

  private readonly _routers = signal<ClientRouter[]>([]);
  private readonly _loading = signal(false);
  private readonly _error = signal<string | null>(null);

  readonly routers = this._routers.asReadonly();
  readonly loading = this._loading.asReadonly();
  readonly error = this._error.asReadonly();

  async fetchRouters(userId: number): Promise<void> {
    this._loading.set(true);
    this._error.set(null);
    try {
      const routers = await firstValueFrom(this.http.get<ClientRouter[]>(`/api/infrastructure/routers/user/${userId}`));
      this._routers.set(Array.isArray(routers) ? routers : []);
    } catch (err: any) {
      this._error.set(err?.error?.message || 'Erreur lors du chargement des routeurs');
    } finally {
      this._loading.set(false);
    }
  }

  async createRouter(data: CreateRouterRequest): Promise<ClientRouter> {
    this._loading.set(true);
    this._error.set(null);
    try {
      const router = await firstValueFrom(this.http.post<ClientRouter>('/api/infrastructure/routers', data));
      this._routers.update((routers) => [...routers, router]);
      return router;
    } catch (err: any) {
      this._error.set(err?.error?.message || 'Erreur lors de la création du routeur');
      throw err;
    } finally {
      this._loading.set(false);
    }
  }

  async listInterfaces(id: number): Promise<RouterInterfaceDetails[]> {
    return firstValueFrom(this.http.get<RouterInterfaceDetails[]>(`/api/infrastructure/routers/${id}/interfaces`));
  }

  async attachInterface(id: number, subnetId: string): Promise<void> {
    try {
      await firstValueFrom(this.http.post(`/api/infrastructure/routers/${id}/interfaces`, { subnetId }));
    } catch (err: any) {
      this._error.set(err?.error?.message || "Erreur lors de l'attachement de l'interface");
      throw err;
    }
  }

  async detachInterface(id: number, subnetId: string): Promise<void> {
    try {
      await firstValueFrom(this.http.delete(`/api/infrastructure/routers/${id}/interfaces/${subnetId}`));
    } catch (err: any) {
      this._error.set(err?.error?.message || "Erreur lors du détachement de l'interface");
      throw err;
    }
  }

  async deleteRouter(id: number): Promise<void> {
    try {
      await firstValueFrom(this.http.delete(`/api/infrastructure/routers/${id}`));
      this._routers.update((routers) => routers.filter((r) => r.id !== id));
    } catch (err: any) {
      this._error.set(err?.error?.message || 'Erreur lors de la suppression du routeur');
      throw err;
    }
  }

  clearError(): void {
    this._error.set(null);
  }
}
