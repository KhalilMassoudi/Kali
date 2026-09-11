import { Injectable, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { firstValueFrom } from 'rxjs';
import { CreateVolumeSnapshotRequest, VpsVolumeSnapshot } from '../models/vm.model';

@Injectable({ providedIn: 'root' })
export class VolumeSnapshotService {
  private readonly http = inject(HttpClient);

  private readonly _snapshots = signal<VpsVolumeSnapshot[]>([]);
  private readonly _loading = signal(false);
  private readonly _error = signal<string | null>(null);

  readonly snapshots = this._snapshots.asReadonly();
  readonly loading = this._loading.asReadonly();
  readonly error = this._error.asReadonly();

  async fetchSnapshots(userId: number): Promise<void> {
    this._loading.set(true);
    this._error.set(null);
    try {
      const snapshots = await firstValueFrom(
        this.http.get<VpsVolumeSnapshot[]>(`/api/infrastructure/volume-snapshots/user/${userId}`),
      );
      this._snapshots.set(Array.isArray(snapshots) ? snapshots : []);
    } catch (err: any) {
      this._error.set(err?.error?.message || 'Erreur lors du chargement des snapshots');
    } finally {
      this._loading.set(false);
    }
  }

  snapshotsForVolume(volumeId: number): VpsVolumeSnapshot[] {
    return this._snapshots().filter((s) => s.sourceVolumeId === volumeId);
  }

  async fetchSnapshotsForVolume(volumeId: number): Promise<void> {
    this._loading.set(true);
    this._error.set(null);
    try {
      const snapshots = await firstValueFrom(
        this.http.get<VpsVolumeSnapshot[]>(`/api/infrastructure/volume-snapshots/volume/${volumeId}`),
      );
      const others = this._snapshots().filter((s) => s.sourceVolumeId !== volumeId);
      this._snapshots.set([...others, ...(Array.isArray(snapshots) ? snapshots : [])]);
    } catch (err: any) {
      this._error.set(err?.error?.message || 'Erreur lors du chargement des snapshots');
    } finally {
      this._loading.set(false);
    }
  }

  async createSnapshot(data: CreateVolumeSnapshotRequest): Promise<VpsVolumeSnapshot> {
    this._loading.set(true);
    this._error.set(null);
    try {
      const snapshot = await firstValueFrom(this.http.post<VpsVolumeSnapshot>('/api/infrastructure/volume-snapshots', data));
      this._snapshots.update((snapshots) => [...snapshots, snapshot]);
      return snapshot;
    } catch (err: any) {
      this._error.set(err?.error?.message || 'Erreur lors de la création du snapshot');
      throw err;
    } finally {
      this._loading.set(false);
    }
  }

  async deleteSnapshot(id: number): Promise<void> {
    try {
      await firstValueFrom(this.http.delete(`/api/infrastructure/volume-snapshots/${id}`));
      this._snapshots.update((snapshots) => snapshots.filter((s) => s.id !== id));
    } catch (err: any) {
      this._error.set(err?.error?.message || 'Erreur lors de la suppression du snapshot');
      throw err;
    }
  }

  clearError(): void {
    this._error.set(null);
  }
}
