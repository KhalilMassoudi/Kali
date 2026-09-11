import { Component, computed, effect, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ButtonModule } from 'primeng/button';
import { AuthService } from '../../core/services/auth.service';
import { VmService } from '../../core/services/vm.service';
import { VpsStatus } from '../../core/models/vm.model';
import { VmTable } from '../../shared/vm-table/vm-table';
import { CreateVmDialog } from './create-vm-dialog/create-vm-dialog';

type FilterKey = 'all' | 'running' | 'stopped' | 'error';

const FILTERS: { key: FilterKey; label: string }[] = [
  { key: 'all', label: 'Toutes' },
  { key: 'running', label: 'En marche' },
  { key: 'stopped', label: 'Arrêtées' },
  { key: 'error', label: 'En erreur' },
];

@Component({
  selector: 'app-vms',
  imports: [CommonModule, ButtonModule, VmTable, CreateVmDialog],
  templateUrl: './vms.html',
  styleUrl: './vms.scss',
})
export class Vms {
  private readonly auth = inject(AuthService);
  readonly vmService = inject(VmService);

  readonly user = this.auth.user;
  readonly vms = this.vmService.vms;
  readonly loading = this.vmService.loading;
  readonly error = this.vmService.error;

  readonly filters = FILTERS;
  readonly filter = signal<FilterKey>('all');
  readonly showCreateDialog = signal(false);

  readonly filtered = computed(() => {
    const key = this.filter();
    const list = this.vms();
    if (key === 'all') return list;
    const target: VpsStatus[] = key === 'running' ? ['RUNNING'] : key === 'stopped' ? ['STOPPED'] : ['ERROR'];
    return list.filter((v) => target.includes(v.status));
  });

  readonly counts = computed(() => {
    const list = this.vms();
    return {
      all: list.length,
      running: list.filter((v) => v.status === 'RUNNING').length,
      stopped: list.filter((v) => v.status === 'STOPPED').length,
      error: list.filter((v) => v.status === 'ERROR').length,
    };
  });

  constructor() {
    effect(() => {
      const user = this.auth.user();
      if (user) this.vmService.fetchVms(user.id);
    });
  }

  refresh(): void {
    const user = this.user();
    if (user) this.vmService.fetchVms(user.id);
  }

  clearError(): void {
    this.vmService.clearError();
  }

  emptyStateLabel(): string {
    if (this.filter() === 'all') return 'Aucune VM pour le moment';
    const f = this.filters.find((f) => f.key === this.filter());
    return `Aucune VM ${f?.label.toLowerCase()}`;
  }
}
