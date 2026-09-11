import { Injectable, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { firstValueFrom } from 'rxjs';
import { AdoptImageRequest, ImageOption, ImportImageUrlRequest, PlatformImage } from '../models/vm.model';

@Injectable({ providedIn: 'root' })
export class ImageService {
  private readonly http = inject(HttpClient);

  private readonly _publishedImages = signal<PlatformImage[]>([]);
  private readonly _allImages = signal<PlatformImage[]>([]);
  private readonly _availableForAdoption = signal<ImageOption[]>([]);
  private readonly _error = signal<string | null>(null);

  readonly publishedImages = this._publishedImages.asReadonly();
  readonly allImages = this._allImages.asReadonly();
  readonly availableForAdoption = this._availableForAdoption.asReadonly();
  readonly error = this._error.asReadonly();

  async fetchPublished(): Promise<void> {
    try {
      const images = await firstValueFrom(this.http.get<PlatformImage[]>('/api/infrastructure/images'));
      this._publishedImages.set(Array.isArray(images) ? images : []);
    } catch {
      this._publishedImages.set([]);
    }
  }

  async fetchAll(): Promise<void> {
    this._error.set(null);
    try {
      const images = await firstValueFrom(this.http.get<PlatformImage[]>('/api/infrastructure/admin/images'));
      this._allImages.set(Array.isArray(images) ? images : []);
    } catch (err: any) {
      this._error.set(err?.error?.message || 'Erreur lors du chargement du catalogue');
    }
  }

  async fetchAvailableForAdoption(): Promise<void> {
    try {
      const images = await firstValueFrom(this.http.get<ImageOption[]>('/api/infrastructure/admin/images/available'));
      this._availableForAdoption.set(Array.isArray(images) ? images : []);
    } catch (err: any) {
      this._error.set(err?.error?.message || 'Erreur lors du chargement des images OpenStack');
    }
  }

  async adopt(data: AdoptImageRequest): Promise<void> {
    try {
      const image = await firstValueFrom(this.http.post<PlatformImage>('/api/infrastructure/admin/images/adopt', data));
      this._allImages.update((images) => [...images, image]);
    } catch (err: any) {
      this._error.set(err?.error?.message || "Erreur lors de l'adoption de l'image");
      throw err;
    }
  }

  async importFromUrl(data: ImportImageUrlRequest): Promise<void> {
    try {
      const image = await firstValueFrom(
        this.http.post<PlatformImage>('/api/infrastructure/admin/images/import-url', data),
      );
      this._allImages.update((images) => [...images, image]);
    } catch (err: any) {
      this._error.set(err?.error?.message || "Erreur lors de l'import de l'image");
      throw err;
    }
  }

  async setVisibility(id: number, visibility: 'PUBLISHED' | 'HIDDEN'): Promise<void> {
    try {
      const updated = await firstValueFrom(
        this.http.patch<PlatformImage>(`/api/infrastructure/admin/images/${id}`, { visibility }),
      );
      this._allImages.update((images) => images.map((i) => (i.id === id ? updated : i)));
    } catch (err: any) {
      this._error.set(err?.error?.message || 'Erreur lors de la mise à jour de la visibilité');
      throw err;
    }
  }

  async deleteImage(id: number): Promise<void> {
    try {
      await firstValueFrom(this.http.delete(`/api/infrastructure/admin/images/${id}`));
      this._allImages.update((images) => images.filter((i) => i.id !== id));
    } catch (err: any) {
      this._error.set(err?.error?.message || "Erreur lors de la suppression de l'image");
      throw err;
    }
  }

  clearError(): void {
    this._error.set(null);
  }
}
