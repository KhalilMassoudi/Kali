import { Injectable, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { firstValueFrom } from 'rxjs';
import { ClientServerGroup, CreateServerGroupRequest, ServerGroupDetails } from '../models/vm.model';

@Injectable({ providedIn: 'root' })
export class ServerGroupService {
  private readonly http = inject(HttpClient);

  private readonly _groups = signal<ClientServerGroup[]>([]);
  private readonly _loading = signal(false);
  private readonly _error = signal<string | null>(null);

  readonly groups = this._groups.asReadonly();
  readonly loading = this._loading.asReadonly();
  readonly error = this._error.asReadonly();

  async fetchGroups(userId: number): Promise<void> {
    this._loading.set(true);
    this._error.set(null);
    try {
      const groups = await firstValueFrom(this.http.get<ClientServerGroup[]>(`/api/infrastructure/server-groups/user/${userId}`));
      this._groups.set(Array.isArray(groups) ? groups : []);
    } catch (err: any) {
      this._error.set(err?.error?.message || 'Erreur lors du chargement des groupes de serveurs');
    } finally {
      this._loading.set(false);
    }
  }

  async createGroup(data: CreateServerGroupRequest): Promise<ClientServerGroup> {
    this._loading.set(true);
    this._error.set(null);
    try {
      const group = await firstValueFrom(this.http.post<ClientServerGroup>('/api/infrastructure/server-groups', data));
      this._groups.update((groups) => [...groups, group]);
      return group;
    } catch (err: any) {
      this._error.set(err?.error?.message || 'Erreur lors de la création du groupe de serveurs');
      throw err;
    } finally {
      this._loading.set(false);
    }
  }

  async getLiveDetails(id: number): Promise<ServerGroupDetails> {
    return firstValueFrom(this.http.get<ServerGroupDetails>(`/api/infrastructure/server-groups/${id}`));
  }

  async deleteGroup(id: number): Promise<void> {
    try {
      await firstValueFrom(this.http.delete(`/api/infrastructure/server-groups/${id}`));
      this._groups.update((groups) => groups.filter((g) => g.id !== id));
    } catch (err: any) {
      this._error.set(err?.error?.message || 'Erreur lors de la suppression du groupe de serveurs');
      throw err;
    }
  }

  clearError(): void {
    this._error.set(null);
  }
}
