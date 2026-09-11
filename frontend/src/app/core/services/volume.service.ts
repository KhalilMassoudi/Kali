import { Injectable, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { firstValueFrom } from 'rxjs';
import { CreateVolumeRequest, Volume } from '../models/vm.model';

@Injectable({ providedIn: 'root' })
export class VolumeService {
  private readonly http = inject(HttpClient);

  private readonly _volumes = signal<Volume[]>([]);
  private readonly _loading = signal(false);
  private readonly _error = signal<string | null>(null);

  readonly volumes = this._volumes.asReadonly();
  readonly loading = this._loading.asReadonly();
  readonly error = this._error.asReadonly();

  async fetchVolumes(userId: number): Promise<void> {
    this._loading.set(true);
    this._error.set(null);
    try {
      const volumes = await firstValueFrom(this.http.get<Volume[]>(`/api/infrastructure/volumes/user/${userId}`));
      this._volumes.set(Array.isArray(volumes) ? volumes : []);
    } catch (err: any) {
      this._error.set(err?.error?.message || 'Erreur lors du chargement des volumes');
    } finally {
      this._loading.set(false);
    }
  }

  async createVolume(data: CreateVolumeRequest): Promise<Volume> {
    this._loading.set(true);
    this._error.set(null);
    try {
      const volume = await firstValueFrom(this.http.post<Volume>('/api/infrastructure/volumes', data));
      this._volumes.update((volumes) => [...volumes, volume]);
      return volume;
    } catch (err: any) {
      this._error.set(err?.error?.message || 'Erreur lors de la création du volume');
      throw err;
    } finally {
      this._loading.set(false);
    }
  }

  async attachVolume(id: number, vpsId: number): Promise<void> {
    try {
      const updated = await firstValueFrom(
        this.http.post<Volume>(`/api/infrastructure/volumes/${id}/attach?vpsId=${vpsId}`, {}),
      );
      this.replaceVolume(updated);
    } catch (err: any) {
      this._error.set(err?.error?.message || "Erreur lors de l'attachement du volume");
      throw err;
    }
  }

  async detachVolume(id: number): Promise<void> {
    try {
      const updated = await firstValueFrom(this.http.post<Volume>(`/api/infrastructure/volumes/${id}/detach`, {}));
      this.replaceVolume(updated);
    } catch (err: any) {
      this._error.set(err?.error?.message || 'Erreur lors du détachement du volume');
      throw err;
    }
  }

  async assignProject(id: number, projectId: number | null): Promise<void> {
    try {
      const updated = await firstValueFrom(this.http.put<Volume>(`/api/infrastructure/volumes/${id}/project`, { projectId }));
      this.replaceVolume(updated);
    } catch (err: any) {
      this._error.set(err?.error?.message || "Erreur lors de l'assignation du projet");
      throw err;
    }
  }

  async deleteVolume(id: number): Promise<void> {
    try {
      await firstValueFrom(this.http.delete(`/api/infrastructure/volumes/${id}`));
      this._volumes.update((volumes) => volumes.filter((v) => v.id !== id));
    } catch (err: any) {
      this._error.set(err?.error?.message || 'Erreur lors de la suppression du volume');
      throw err;
    }
  }

  clearError(): void {
    this._error.set(null);
  }

  private replaceVolume(updated: Volume): void {
    this._volumes.update((volumes) => volumes.map((v) => (v.id === updated.id ? updated : v)));
  }
}
