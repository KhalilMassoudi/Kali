import { Component, computed, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { ButtonModule } from 'primeng/button';
import { TableModule } from 'primeng/table';
import { TagModule } from 'primeng/tag';
import { SelectModule } from 'primeng/select';
import { InputTextModule } from 'primeng/inputtext';
import { ProjectService } from '../../../core/services/project.service';
import { AdminService } from '../../../core/services/admin.service';
import { MetricsService } from '../../../core/services/metrics.service';
import { VmTable } from '../../../shared/vm-table/vm-table';
import { formatRam } from '../../../shared/utils/vm-status.util';
import { AdminImages } from '../images/images';
import { CreateVmDialog } from '../../vms/create-vm-dialog/create-vm-dialog';
import { CreateKeypairDialog } from '../../keypairs/create-keypair-dialog/create-keypair-dialog';

type Tab = 'instances' | 'images' | 'keypairs' | 'server-groups';

const POLICY_LABELS: Record<string, string> = {
  AFFINITY: 'Affinité',
  ANTI_AFFINITY: 'Anti-affinité',
  SOFT_AFFINITY: 'Affinité souple',
  SOFT_ANTI_AFFINITY: 'Anti-affinité souple',
};

@Component({
  selector: 'app-admin-compute',
  imports: [CommonModule, FormsModule, ButtonModule, TableModule, TagModule, SelectModule, InputTextModule, VmTable, AdminImages, CreateVmDialog, CreateKeypairDialog],
  templateUrl: './compute.html',
  styleUrl: './compute.scss',
})
export class AdminCompute {
  readonly admin = inject(AdminService);
  readonly metricsService = inject(MetricsService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly projectService = inject(ProjectService);

  readonly tab = signal<Tab>('instances');
  readonly showCreateVmDialog = signal(false);
  readonly showCreateKeypairDialog = signal(false);
  readonly loading = signal(true);
  readonly error = this.admin.error;

  readonly filterUserId = signal<number | null>(null);

  /** Projects are per client, so the project filter only applies once a client is chosen. */
  readonly filterProjectId = signal<number | null>(null);
  readonly filterSearch = signal('');
  readonly projectFilterOptions = signal<{ label: string; value: number }[]>([]);
  readonly clientFilterOptions = computed(() =>
    this.admin.users().map((u) => ({ label: `${u.email}${u.firstName ? ' — ' + u.firstName : ''}`, value: u.id })),
  );
  readonly hasFilters = computed(() => !!this.filterUserId() || !!this.filterProjectId() || !!this.filterSearch().trim());

  readonly instances = computed(() => {
    const uid = this.filterUserId();
    const pid = this.filterProjectId();
    const q = this.filterSearch().trim().toLowerCase();
    return this.admin.allVps().filter((v) =>
      (!uid || v.userId === uid)
      && (!pid || v.projectId === pid)
      && (!q
        || (v.ipAddress ?? '').toLowerCase().includes(q)
        || (v.floatingIp ?? '').toLowerCase().includes(q)
        || v.name.toLowerCase().includes(q)),
    );
  });
  readonly keypairs = computed(() => {
    const uid = this.filterUserId();
    return uid ? this.admin.allKeypairs().filter((k) => k.userId === uid) : this.admin.allKeypairs();
  });
  readonly serverGroups = this.admin.allServerGroups;
  readonly fleetMetrics = this.metricsService.fleetSummary;

  readonly formatRam = formatRam;
  readonly policyLabel = (policy: string) => POLICY_LABELS[policy] ?? policy;

  constructor() {
    const initial = this.route.snapshot.queryParamMap.get('tab') as Tab | null;
    if (initial) this.tab.set(initial);
    const userId = this.route.snapshot.queryParamMap.get('userId');
    if (userId) this.setUserFilter(Number(userId));
    this.load();
  }

  setUserFilter(userId: number | null): void {
    this.filterUserId.set(userId);
    this.filterProjectId.set(null);
    this.projectFilterOptions.set([]);
    this.router.navigate([], { relativeTo: this.route, queryParams: { userId }, queryParamsHandling: 'merge' });
    if (userId) {
      this.projectService.listForUser(userId).then((projects) => {
        if (this.filterUserId() !== userId) return;
        this.projectFilterOptions.set(projects.map((p) => ({ label: p.name, value: p.id })));
      });
    }
  }

  clearFilters(): void {
    this.filterSearch.set('');
    this.setUserFilter(null);
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
        this.admin.users().length === 0 ? this.admin.loadUsers() : Promise.resolve(),
      ]);
    } finally {
      this.loading.set(false);
    }
  }
}
