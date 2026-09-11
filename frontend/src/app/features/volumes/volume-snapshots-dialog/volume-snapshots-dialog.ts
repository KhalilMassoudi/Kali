import { Component, EventEmitter, Output, computed, effect, inject, input, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { DialogModule } from 'primeng/dialog';
import { InputTextModule } from 'primeng/inputtext';
import { TagModule } from 'primeng/tag';
import { ButtonModule } from 'primeng/button';
import { MessageModule } from 'primeng/message';
import { AuthService } from '../../../core/services/auth.service';
import { VolumeSnapshotService } from '../../../core/services/volume-snapshot.service';
import { Volume } from '../../../core/models/vm.model';

@Component({
  selector: 'app-volume-snapshots-dialog',
  imports: [CommonModule, FormsModule, DialogModule, InputTextModule, TagModule, ButtonModule, MessageModule],
  templateUrl: './volume-snapshots-dialog.html',
  styleUrl: './volume-snapshots-dialog.scss',
})
export class VolumeSnapshotsDialog {
  private readonly auth = inject(AuthService);
  readonly snapshotService = inject(VolumeSnapshotService);

  readonly volume = input<Volume | null>(null);
  @Output() readonly closed = new EventEmitter<void>();

  readonly visible = computed(() => this.volume() !== null);
  readonly snapshots = computed(() => {
    const v = this.volume();
    return v ? this.snapshotService.snapshotsForVolume(v.id) : [];
  });

  readonly name = signal('');
  readonly description = signal('');
  readonly creating = signal(false);
  readonly error = signal('');
  private readonly rowDeleting = signal<Record<number, boolean>>({});

  constructor() {
    effect(() => {
      const v = this.volume();
      if (v) {
        this.error.set('');
        this.snapshotService.fetchSnapshotsForVolume(v.id);
      }
    });
  }

  isDeleting(id: number): boolean {
    return this.rowDeleting()[id] ?? false;
  }

  onHide(): void {
    this.closed.emit();
  }

  async createSnapshot(): Promise<void> {
    const v = this.volume();
    if (!v || !this.name().trim()) return;
    this.error.set('');
    this.creating.set(true);
    try {
      await this.snapshotService.createSnapshot({
        userId: this.auth.user()?.id ?? v.userId,
        volumeId: v.id,
        name: this.name().trim(),
        description: this.description().trim() || undefined,
      });
      this.name.set('');
      this.description.set('');
    } catch {
      this.error.set(this.snapshotService.error() || 'Erreur lors de la création du snapshot');
    } finally {
      this.creating.set(false);
    }
  }

  async remove(id: number): Promise<void> {
    this.rowDeleting.update((s) => ({ ...s, [id]: true }));
    try {
      await this.snapshotService.deleteSnapshot(id);
    } finally {
      this.rowDeleting.update((s) => ({ ...s, [id]: false }));
    }
  }
}
