import { Component, EventEmitter, Output, computed, inject, input, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { TableModule } from 'primeng/table';
import { ButtonModule } from 'primeng/button';
import { TagModule } from 'primeng/tag';
import { TooltipModule } from 'primeng/tooltip';
import { Vm } from '../../core/models/vm.model';
import { VmService } from '../../core/services/vm.service';
import { PreferencesService } from '../../core/services/preferences.service';
import { VmHealthDialog } from '../vm-health-dialog/vm-health-dialog';
import { formatRam, vmStatusLabel, vmStatusSeverity } from '../utils/vm-status.util';

interface RowState {
  confirmDelete: boolean;
  actionLoading: 'stop' | 'start' | 'restart' | 'refresh' | 'delete' | null;
}

@Component({
  selector: 'app-vm-table',
  imports: [CommonModule, RouterLink, TableModule, ButtonModule, TagModule, TooltipModule, VmHealthDialog],
  templateUrl: './vm-table.html',
  styleUrl: './vm-table.scss',
})
export class VmTable {
  private readonly vmService = inject(VmService);
  readonly preferences = inject(PreferencesService);

  readonly vms = input.required<Vm[]>();
  readonly showStorage = input(false);

  @Output() readonly deleted = new EventEmitter<void>();

  private readonly rowStates = signal<Record<number, RowState>>({});

  readonly healthVm = signal<Vm | null>(null);
  readonly healthVisible = signal(false);

  readonly formatRam = formatRam;
  readonly statusLabel = vmStatusLabel;
  readonly statusSeverity = vmStatusSeverity;

  isLive(vm: Vm): boolean {
    return vm.status === 'RUNNING';
  }

  isDim(vm: Vm): boolean {
    return vm.status === 'STOPPED' || vm.status === 'ERROR' || vm.status === 'DELETED';
  }

  rowState(vm: Vm): RowState {
    return this.rowStates()[vm.id] ?? { confirmDelete: false, actionLoading: null };
  }

  private patchRow(id: number, patch: Partial<RowState>): void {
    this.rowStates.update((states) => ({
      ...states,
      [id]: { ...(states[id] ?? { confirmDelete: false, actionLoading: null }), ...patch },
    }));
  }

  askDelete(vm: Vm): void {
    this.patchRow(vm.id, { confirmDelete: true });
  }

  cancelDelete(vm: Vm): void {
    this.patchRow(vm.id, { confirmDelete: false });
  }

  async stop(vm: Vm): Promise<void> {
    this.patchRow(vm.id, { actionLoading: 'stop' });
    try {
      await this.vmService.stopVm(vm.id);
    } finally {
      this.patchRow(vm.id, { actionLoading: null });
    }
  }

  async start(vm: Vm): Promise<void> {
    this.patchRow(vm.id, { actionLoading: 'start' });
    try {
      await this.vmService.startVm(vm.id);
    } finally {
      this.patchRow(vm.id, { actionLoading: null });
    }
  }

  async restart(vm: Vm): Promise<void> {
    this.patchRow(vm.id, { actionLoading: 'restart' });
    try {
      await this.vmService.restartVm(vm.id);
    } finally {
      this.patchRow(vm.id, { actionLoading: null });
    }
  }

  async refreshStatus(vm: Vm): Promise<void> {
    this.patchRow(vm.id, { actionLoading: 'refresh' });
    try {
      await this.vmService.refreshVmStatus(vm.id);
    } finally {
      this.patchRow(vm.id, { actionLoading: null });
    }
  }

  openHealth(vm: Vm): void {
    this.healthVm.set(vm);
    this.healthVisible.set(true);
  }

  async confirmDelete(vm: Vm): Promise<void> {
    this.patchRow(vm.id, { actionLoading: 'delete' });
    try {
      await this.vmService.deleteVm(vm.id);
      this.deleted.emit();
    } finally {
      this.patchRow(vm.id, { actionLoading: null, confirmDelete: false });
    }
  }
}
