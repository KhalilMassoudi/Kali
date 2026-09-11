import { Component, EventEmitter, Output, computed, inject, input, model, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { DialogModule } from 'primeng/dialog';
import { SelectModule } from 'primeng/select';
import { ButtonModule } from 'primeng/button';
import { MessageModule } from 'primeng/message';
import { FloatingIpService } from '../../../core/services/floating-ip.service';
import { ClientFloatingIp, Vm } from '../../../core/models/vm.model';

@Component({
  selector: 'app-associate-floating-ip-dialog',
  imports: [CommonModule, FormsModule, DialogModule, SelectModule, ButtonModule, MessageModule],
  templateUrl: './associate-floating-ip-dialog.html',
  styleUrl: './associate-floating-ip-dialog.scss',
})
export class AssociateFloatingIpDialog {
  private readonly floatingIpService = inject(FloatingIpService);

  readonly floatingIp = input<ClientFloatingIp | null>(null);
  readonly vms = input<Vm[]>([]);
  @Output() readonly closed = new EventEmitter<void>();

  readonly visible = computed(() => this.floatingIp() !== null);
  readonly loading = this.floatingIpService.loading;
  readonly error = signal('');
  readonly selectedVmId = signal<number | null>(null);

  readonly vmOptions = computed(() => this.vms().map((v) => ({ label: v.name, value: v.id })));

  onHide(): void {
    this.error.set('');
    this.selectedVmId.set(null);
    this.closed.emit();
  }

  async submit(): Promise<void> {
    const ip = this.floatingIp();
    const vpsId = this.selectedVmId();
    if (!ip || !vpsId) return;
    this.error.set('');
    try {
      await this.floatingIpService.associate(ip.id, vpsId);
      this.onHide();
    } catch {
      this.error.set(this.floatingIpService.error() || "Erreur lors de l'association");
    }
  }
}
