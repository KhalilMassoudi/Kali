import { Component, EventEmitter, Output, computed, inject, input, model, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { DialogModule } from 'primeng/dialog';
import { SelectModule } from 'primeng/select';
import { ButtonModule } from 'primeng/button';
import { MessageModule } from 'primeng/message';
import { Volume } from '../../../core/models/vm.model';
import { VmService } from '../../../core/services/vm.service';
import { VolumeService } from '../../../core/services/volume.service';

@Component({
  selector: 'app-attach-volume-dialog',
  imports: [CommonModule, FormsModule, DialogModule, SelectModule, ButtonModule, MessageModule],
  templateUrl: './attach-volume-dialog.html',
  styleUrl: './attach-volume-dialog.scss',
})
export class AttachVolumeDialog {
  private readonly vmService = inject(VmService);
  private readonly volumeService = inject(VolumeService);

  readonly volume = input<Volume | null>(null);
  readonly visible = model<boolean>(false);
  @Output() readonly attached = new EventEmitter<void>();

  readonly vmOptions = computed(() => this.vmService.vms().filter((v) => v.status === 'RUNNING' || v.status === 'STOPPED'));
  readonly selectedVpsId = signal<number | null>(null);
  readonly loading = signal(false);
  readonly error = signal('');

  close(): void {
    this.visible.set(false);
    this.selectedVpsId.set(null);
    this.error.set('');
  }

  async attach(): Promise<void> {
    const vol = this.volume();
    const vpsId = this.selectedVpsId();
    if (!vol || !vpsId) return;
    this.loading.set(true);
    this.error.set('');
    try {
      await this.volumeService.attachVolume(vol.id, vpsId);
      this.attached.emit();
      this.close();
    } catch {
      this.error.set(this.volumeService.error() || "Erreur lors de l'attachement");
    } finally {
      this.loading.set(false);
    }
  }
}
