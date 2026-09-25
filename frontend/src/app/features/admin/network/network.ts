import { Component, computed, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { ButtonModule } from 'primeng/button';
import { TableModule } from 'primeng/table';
import { TagModule } from 'primeng/tag';
import { SelectModule } from 'primeng/select';
import { AdminService } from '../../../core/services/admin.service';
import { CreateNetworkDialog } from '../../networks/create-network-dialog/create-network-dialog';
import { CreateSecurityGroupDialog } from '../../security-groups/create-security-group-dialog/create-security-group-dialog';
import { CreateRouterDialog } from '../../network-hub/create-router-dialog/create-router-dialog';
import { AllocateFloatingIpDialog } from '../../network-hub/allocate-floating-ip-dialog/allocate-floating-ip-dialog';
import { NetworkOption } from '../../../core/models/vm.model';
import { VmService } from '../../../core/services/vm.service';
import { FloatingIpService } from '../../../core/services/floating-ip.service';

type Tab = 'networks' | 'security-groups' | 'routers' | 'floating-ips';

@Component({
  selector: 'app-admin-network',
  imports: [
    CommonModule,
    FormsModule,
    ButtonModule,
    TableModule,
    TagModule,
    SelectModule,
    CreateNetworkDialog,
    CreateSecurityGroupDialog,
    CreateRouterDialog,
    AllocateFloatingIpDialog,
  ],
  templateUrl: './network.html',
  styleUrl: './network.scss',
})
export class AdminNetwork {
  readonly admin = inject(AdminService);
  private readonly vmService = inject(VmService);
  private readonly floatingIpService = inject(FloatingIpService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);

  readonly tab = signal<Tab>('networks');
  readonly loading = signal(true);
  readonly error = this.admin.error;

  readonly filterUserId = signal<number | null>(null);
  readonly filterUserLabel = computed(() => {
    const uid = this.filterUserId();
    if (!uid) return null;
    const user = this.admin.users().find((u) => u.id === uid);
    return user ? user.email : `#${uid}`;
  });

  readonly networks = computed(() => {
    const uid = this.filterUserId();
    return uid ? this.admin.allNetworks().filter((n) => n.userId === uid) : this.admin.allNetworks();
  });
  readonly securityGroups = computed(() => {
    const uid = this.filterUserId();
    return uid ? this.admin.allSecurityGroups().filter((g) => g.userId === uid) : this.admin.allSecurityGroups();
  });
  readonly routers = computed(() => {
    const uid = this.filterUserId();
    return uid ? this.admin.allRouters().filter((r) => r.userId === uid) : this.admin.allRouters();
  });
  readonly floatingIps = computed(() => {
    const uid = this.filterUserId();
    return uid ? this.admin.allFloatingIps().filter((f) => f.userId === uid) : this.admin.allFloatingIps();
  });
  readonly externalNetworks = signal<NetworkOption[]>([]);
  readonly pools = signal<string[]>([]);

  readonly targetUserId = signal<number | null>(null);
  readonly clientOptions = computed(() =>
    this.admin.users().map((u) => ({ label: `${u.email}${u.firstName ? ' — ' + u.firstName : ''}`, value: u.id })),
  );

  readonly showCreateNetworkDialog = signal(false);
  readonly showCreateGroupDialog = signal(false);
  readonly showCreateRouterDialog = signal(false);
  readonly showAllocateFloatingIpDialog = signal(false);

  constructor() {
    const initial = this.route.snapshot.queryParamMap.get('tab') as Tab | null;
    if (initial) this.tab.set(initial);
    const userId = this.route.snapshot.queryParamMap.get('userId');
    if (userId) this.filterUserId.set(Number(userId));
    if (this.admin.users().length === 0) this.admin.loadUsers();
    this.load();
  }

  clearUserFilter(): void {
    this.filterUserId.set(null);
    this.router.navigate([], { relativeTo: this.route, queryParams: { userId: null }, queryParamsHandling: 'merge' });
  }

  openCreateNetwork(): void {
    this.showCreateNetworkDialog.set(true);
  }

  openCreateGroup(): void {
    this.showCreateGroupDialog.set(true);
  }

  vmName(vpsId: number | null): string {
    if (!vpsId) return '—';
    return this.admin.allVps().find((v) => v.id === vpsId)?.name ?? `VM #${vpsId}`;
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
        this.admin.loadAllNetworks(),
        this.admin.loadAllSecurityGroups(),
        this.admin.loadAllRouters(),
        this.admin.loadAllFloatingIps(),
        this.admin.loadAllVps(),
        this.vmService.fetchNetworks().then((nets) => this.externalNetworks.set(nets.filter((n) => n.external))),
        this.floatingIpService.fetchPools().then((pools) => this.pools.set(pools)),
      ]);
    } finally {
      this.loading.set(false);
    }
  }
}
