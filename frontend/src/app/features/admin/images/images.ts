import { Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ButtonModule } from 'primeng/button';
import { TableModule } from 'primeng/table';
import { TagModule } from 'primeng/tag';
import { SelectModule } from 'primeng/select';
import { InputTextModule } from 'primeng/inputtext';
import { InputNumberModule } from 'primeng/inputnumber';
import { MessageModule } from 'primeng/message';
import { ImageService } from '../../../core/services/image.service';
import { ImageOption, PlatformImage } from '../../../core/models/vm.model';

type Mode = 'adopt' | 'import';

@Component({
  selector: 'app-admin-images',
  imports: [CommonModule, FormsModule, ButtonModule, TableModule, TagModule, SelectModule, InputTextModule, InputNumberModule, MessageModule],
  templateUrl: './images.html',
  styleUrl: './images.scss',
})
export class AdminImages {
  readonly imageService = inject(ImageService);

  readonly images = this.imageService.allImages;
  readonly available = this.imageService.availableForAdoption;
  readonly error = this.imageService.error;
  readonly loading = signal(true);

  readonly mode = signal<Mode>('adopt');
  readonly saving = signal(false);

  readonly adoptTarget = signal<ImageOption | null>(null);
  readonly displayName = signal('');
  readonly osDistro = signal('');
  readonly osVersion = signal('');
  readonly imageUrl = signal('');
  readonly diskFormat = signal('qcow2');
  readonly minDiskGb = signal<number | null>(10);
  readonly minRamMb = signal<number | null>(512);

  constructor() {
    this.load();
  }

  private async load(): Promise<void> {
    this.loading.set(true);
    try {
      await Promise.all([this.imageService.fetchAll(), this.imageService.fetchAvailableForAdoption()]);
    } finally {
      this.loading.set(false);
    }
  }

  async refresh(): Promise<void> {
    await this.load();
  }

  async adopt(): Promise<void> {
    const target = this.adoptTarget();
    if (!target || !this.displayName()) return;
    this.saving.set(true);
    try {
      await this.imageService.adopt({
        externalId: target.id,
        displayName: this.displayName(),
        osDistro: this.osDistro() || undefined,
        osVersion: this.osVersion() || undefined,
      });
      await this.imageService.fetchAvailableForAdoption();
      this.resetForm();
    } finally {
      this.saving.set(false);
    }
  }

  async importFromUrl(): Promise<void> {
    if (!this.displayName() || !this.imageUrl()) return;
    this.saving.set(true);
    try {
      await this.imageService.importFromUrl({
        displayName: this.displayName(),
        osDistro: this.osDistro() || undefined,
        osVersion: this.osVersion() || undefined,
        imageUrl: this.imageUrl(),
        diskFormat: this.diskFormat(),
        minDiskGb: this.minDiskGb() ?? undefined,
        minRamMb: this.minRamMb() ?? undefined,
      });
      this.resetForm();
    } finally {
      this.saving.set(false);
    }
  }

  async togglePublish(image: PlatformImage): Promise<void> {
    await this.imageService.setVisibility(image.id, image.visibility === 'PUBLISHED' ? 'HIDDEN' : 'PUBLISHED');
  }

  async remove(image: PlatformImage): Promise<void> {
    await this.imageService.deleteImage(image.id);
  }

  private resetForm(): void {
    this.adoptTarget.set(null);
    this.displayName.set('');
    this.osDistro.set('');
    this.osVersion.set('');
    this.imageUrl.set('');
    this.minDiskGb.set(10);
    this.minRamMb.set(512);
  }
}
