import { Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';
import { ButtonModule } from 'primeng/button';
import { TableModule } from 'primeng/table';
import { TagModule } from 'primeng/tag';
import { AdminService } from '../../../core/services/admin.service';
import { MetricsService } from '../../../core/services/metrics.service';
import { VmTable } from '../../../shared/vm-table/vm-table';
import { formatRam } from '../../../shared/utils/vm-status.util';
import { AdminImages } from '../images/images';
import { CreateVmDialog } from '../../vms/create-vm-dialog/create-vm-dialog';

type Tab = 'instances' | 'images' | 'keypairs' | 'server-groups';

const POLICY_LABELS: Record<string, string> = {
  AFFINITY: 'Affinité',
  ANTI_AFFINITY: 'Anti-affinité',
  SOFT_AFFINITY: 'Affinité souple',
  SOFT_ANTI_AFFINITY: 'Anti-affinité souple',
};

@Component({
  selector: 'app-admin-compute',
  imports: [CommonModule, ButtonModule, TableModule, TagModule, VmTable, AdminImages, CreateVmDialog],
  templateUrl: './compute.html',
  styleUrl: './compute.scss',
})
export class AdminCompute {
  readonly admin = inject(AdminService);
  readonly metricsService = inject(MetricsService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);

  readonly tab = signal<Tab>('instances');
  readonly showCreateVmDialog = signal(false);
  readonly loading = signal(true);
  readonly error = this.admin.error;

  readonly instances = this.admin.allVps;
  readonly keypairs = this.admin.allKeypairs;
  readonly serverGroups = this.admin.allServerGroups;
  readonly fleetMetrics = this.metricsService.fleetSummary;

  readonly formatRam = formatRam;
  readonly policyLabel = (policy: string) => POLICY_LABELS[policy] ?? policy;

  constructor() {
    const initial = this.route.snapshot.queryParamMap.get('tab') as Tab | null;
    if (initial) this.tab.set(initial);
    this.load();
  }

  selectTab(tab: Tab): void {
    this.tab.set(tab);
    this.router.navigate([], { relativeTo: this.route, queryParams: { tab }, queryParamsHandling: 'merge' });
  }

  async refresh(): Promise<void> {
    await this.load();
  }

  private async load(): Promise<void> {
    this.loading.set(true);
    try {
      await Promise.all([
        this.admin.loadAllVps(),
        this.admin.loadAllKeypairs(),
        this.admin.loadAllServerGroups(),
        this.metricsService.loadFleetSummary(),
      ]);
    } finally {
      this.loading.set(false);
    }
  }
}
