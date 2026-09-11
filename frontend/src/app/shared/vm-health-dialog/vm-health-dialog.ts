import { Component, effect, inject, input, model, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { DialogModule } from 'primeng/dialog';
import { ButtonModule } from 'primeng/button';
import { TagModule } from 'primeng/tag';
import { Vm, VmHealth } from '../../core/models/vm.model';
import { VmService } from '../../core/services/vm.service';
import { vmStatusLabel, vmStatusSeverity } from '../utils/vm-status.util';

@Component({
  selector: 'app-vm-health-dialog',
  imports: [CommonModule, DialogModule, ButtonModule, TagModule],
  templateUrl: './vm-health-dialog.html',
  styleUrl: './vm-health-dialog.scss',
})
export class VmHealthDialog {
  private readonly vmService = inject(VmService);

  readonly vm = input<Vm | null>(null);
  readonly visible = model<boolean>(false);

  readonly statusLabel = vmStatusLabel;
  readonly statusSeverity = vmStatusSeverity;

  readonly loading = signal(false);
  readonly error = signal<string | null>(null);
  readonly health = signal<VmHealth | null>(null);
  readonly ipActionLoading = signal(false);
  readonly ipActionError = this.vmService.error;

  readonly diagnosticsEntries = signal<[string, number][]>([]);

  constructor() {
    effect(() => {
      if (this.visible() && this.vm()) {
        this.load();
      } else if (!this.visible()) {
        this.health.set(null);
        this.error.set(null);
      }
    });
  }

  private async load(): Promise<void> {
    const vm = this.vm();
    if (!vm) return;
    this.loading.set(true);
    this.error.set(null);
    try {
      const health = await this.vmService.getVmHealth(vm.id);
      this.health.set(health);
      this.diagnosticsEntries.set(Object.entries(health.diagnostics ?? {}));
    } catch (err: any) {
      this.error.set(err?.error?.message || 'Impossible de récupérer le statut OpenStack en direct');
    } finally {
      this.loading.set(false);
    }
  }

  refresh(): void {
    this.load();
  }

  async allocateFloatingIp(): Promise<void> {
    const vm = this.vm();
    if (!vm) return;
    this.ipActionLoading.set(true);
    try {
      await this.vmService.allocateFloatingIp(vm.id);
      await this.load();
    } catch {
      // error already surfaced via vmService.error(); nothing else to do here
    } finally {
      this.ipActionLoading.set(false);
    }
  }

  async releaseFloatingIp(): Promise<void> {
    const vm = this.vm();
    if (!vm) return;
    this.ipActionLoading.set(true);
    try {
      await this.vmService.releaseFloatingIp(vm.id);
      await this.load();
    } catch {
      // error already surfaced via vmService.error(); nothing else to do here
    } finally {
      this.ipActionLoading.set(false);
    }
  }

  close(): void {
    this.visible.set(false);
  }
}
