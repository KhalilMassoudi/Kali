import { Component, computed, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router as NgRouter } from '@angular/router';
import { ButtonModule } from 'primeng/button';
import { TableModule } from 'primeng/table';
import { TagModule } from 'primeng/tag';
import { TooltipModule } from 'primeng/tooltip';
import { SelectModule } from 'primeng/select';
import { FormsModule } from '@angular/forms';
import { AuthService } from '../../core/services/auth.service';
import { NetworkService } from '../../core/services/network.service';
import { SecurityGroupService } from '../../core/services/security-group.service';
import { RouterService } from '../../core/services/router.service';
import { FloatingIpService } from '../../core/services/floating-ip.service';
import { VmService } from '../../core/services/vm.service';
import { ClientFloatingIp, ClientRouter, NetworkOption } from '../../core/models/vm.model';
import { Networks } from '../networks/networks';
import { SecurityGroups } from '../security-groups/security-groups';
import { NetworkTopology, TopologyNetwork, TopologyRouter } from '../../shared/network-topology/network-topology';
import { CreateRouterDialog } from './create-router-dialog/create-router-dialog';
import { AllocateFloatingIpDialog } from './allocate-floating-ip-dialog/allocate-floating-ip-dialog';
import { AssociateFloatingIpDialog } from './associate-floating-ip-dialog/associate-floating-ip-dialog';

type Tab = 'topology' | 'networks' | 'routers' | 'security-groups' | 'floating-ips';

interface RouterRowState {
  deleting: boolean;
  editingGateway: boolean;
  gatewaySelection: string | null;
  savingGateway: boolean;
}

interface FloatingIpRowState {
  busy: boolean;
}

@Component({
  selector: 'app-network-hub',
  imports: [
    CommonModule,
    ButtonModule,
    TableModule,
    TagModule,
    TooltipModule,
    SelectModule,
    FormsModule,
    Networks,
    SecurityGroups,
    NetworkTopology,
    CreateRouterDialog,
    AllocateFloatingIpDialog,
    AssociateFloatingIpDialog,
  ],
  templateUrl: './network-hub.html',
  styleUrl: './network-hub.scss',
})
export class NetworkHub {
  private readonly auth = inject(AuthService);
  private readonly networkService = inject(NetworkService);
  readonly routerService = inject(RouterService);
  readonly floatingIpService = inject(FloatingIpService);
  private readonly vmService = inject(VmService);
  private readonly route = inject(ActivatedRoute);
  private readonly ngRouter = inject(NgRouter);

  readonly tab = signal<Tab>('topology');
  readonly loading = signal(true);

  readonly routers = this.routerService.routers;
  readonly floatingIps = this.floatingIpService.floatingIps;
  readonly vms = this.vmService.vms;
  readonly catalogNetworks = signal<NetworkOption[]>([]);
  readonly pools = signal<string[]>([]);

  readonly showCreateRouterDialog = signal(false);
  readonly showAllocateFloatingIpDialog = signal(false);
  readonly associateTarget = signal<ClientFloatingIp | null>(null);

  private readonly routerRowStates = signal<Record<number, RouterRowState>>({});
  private readonly floatingIpRowStates = signal<Record<number, FloatingIpRowState>>({});

  readonly externalNetworkOptions = computed(() => this.catalogNetworks().filter((n) => n.external));

  readonly topologyNetworks = computed<TopologyNetwork[]>(() => {
    const external = this.catalogNetworks()
      .filter((n) => n.external)
      .map((n) => ({ id: n.id, name: n.name, external: true }));
    const privateNets = this.networkService.networks().map((n) => ({ id: n.externalId ?? String(n.id), name: n.name, external: false }));
    return [...external, ...privateNets];
  });

  private readonly routerInterfaceSubnets = signal<Record<string, string[]>>({});

  readonly topologyRouters = computed<TopologyRouter[]>(() => {
    const subnetToNetwork = new Map(this.networkService.networks().map((n) => [n.subnetId, n.externalId ?? String(n.id)]));
    return this.routers().map((r) => {
      const subnets = this.routerInterfaceSubnets()[r.externalId] ?? [];
      const connectedNetworkIds = subnets
        .map((subnetId) => subnetToNetwork.get(subnetId))
        .filter((id): id is string => !!id);
      return { id: r.externalId, name: r.name, gatewayNetworkId: r.externalGatewayNetworkId, connectedNetworkIds };
    });
  });

  constructor() {
    const initial = this.route.snapshot.queryParamMap.get('tab') as Tab | null;
    if (initial) this.tab.set(initial);
    this.load();
  }

  selectTab(tab: Tab): void {
    this.tab.set(tab);
    this.ngRouter.navigate([], { relativeTo: this.route, queryParams: { tab }, queryParamsHandling: 'merge' });
    if (tab === 'topology') this.loadRouterInterfaces();
  }

  refresh(): void {
    this.load();
  }

  async assignFloatingIp(ip: ClientFloatingIp): Promise<void> {
    this.associateTarget.set(ip);
  }

  async disassociateFloatingIp(ip: ClientFloatingIp): Promise<void> {
    this.floatingIpRowStates.update((s) => ({ ...s, [ip.id]: { busy: true } }));
    try {
      await this.floatingIpService.disassociate(ip.id);
    } finally {
      this.floatingIpRowStates.update((s) => ({ ...s, [ip.id]: { busy: false } }));
    }
  }

  async releaseFloatingIp(ip: ClientFloatingIp): Promise<void> {
    this.floatingIpRowStates.update((s) => ({ ...s, [ip.id]: { busy: true } }));
    try {
      await this.floatingIpService.release(ip.id);
    } finally {
      this.floatingIpRowStates.update((s) => ({ ...s, [ip.id]: { busy: false } }));
    }
  }

  floatingIpRowState(ip: ClientFloatingIp): FloatingIpRowState {
    return this.floatingIpRowStates()[ip.id] ?? { busy: false };
  }

  vmName(vpsId: number | null): string {
    if (!vpsId) return '—';
    return this.vms().find((v) => v.id === vpsId)?.name ?? `VM #${vpsId}`;
  }

  private readonly defaultRouterRowState: RouterRowState = {
    deleting: false,
    editingGateway: false,
    gatewaySelection: null,
    savingGateway: false,
  };

  routerRowState(router: ClientRouter): RouterRowState {
    return this.routerRowStates()[router.id] ?? this.defaultRouterRowState;
  }

  toggleGatewayEdit(router: ClientRouter): void {
    const current = this.routerRowState(router);
    this.routerRowStates.update((s) => ({
      ...s,
      [router.id]: { ...current, editingGateway: !current.editingGateway, gatewaySelection: router.externalGatewayNetworkId ?? null },
    }));
  }

  setGatewaySelection(router: ClientRouter, networkId: string | null): void {
    const current = this.routerRowState(router);
    this.routerRowStates.update((s) => ({ ...s, [router.id]: { ...current, gatewaySelection: networkId } }));
  }

  async saveGateway(router: ClientRouter): Promise<void> {
    const current = this.routerRowState(router);
    if (!current.gatewaySelection) return;
    this.routerRowStates.update((s) => ({ ...s, [router.id]: { ...current, savingGateway: true } }));
    try {
      await this.routerService.setGateway(router.id, current.gatewaySelection);
      this.routerRowStates.update((s) => ({
        ...s,
        [router.id]: { ...this.defaultRouterRowState },
      }));
    } finally {
      this.routerRowStates.update((s) => ({ ...s, [router.id]: { ...this.routerRowState(router), savingGateway: false } }));
    }
  }

  async clearGateway(router: ClientRouter): Promise<void> {
    const current = this.routerRowState(router);
    this.routerRowStates.update((s) => ({ ...s, [router.id]: { ...current, savingGateway: true } }));
    try {
      await this.routerService.clearGateway(router.id);
    } finally {
      this.routerRowStates.update((s) => ({ ...s, [router.id]: { ...this.defaultRouterRowState } }));
    }
  }

  async removeRouter(router: ClientRouter): Promise<void> {
    this.routerRowStates.update((s) => ({ ...s, [router.id]: { ...this.defaultRouterRowState, deleting: true } }));
    try {
      await this.routerService.deleteRouter(router.id);
    } finally {
      this.routerRowStates.update((s) => ({ ...s, [router.id]: { ...this.defaultRouterRowState, deleting: false } }));
    }
  }

  private async load(): Promise<void> {
    this.loading.set(true);
    const user = this.auth.user();
    try {
      const tasks: Promise<unknown>[] = [
        this.routerService.fetchRouters(user?.id ?? 0),
        this.floatingIpService.fetchFloatingIps(user?.id ?? 0),
        this.vmService.fetchVms(user?.id ?? 0),
        this.vmService.fetchNetworks().then((nets) => this.catalogNetworks.set(nets)),
        this.floatingIpService.fetchPools().then((pools) => this.pools.set(pools)),
      ];
      if (user) tasks.push(this.networkService.fetchNetworks(user.id));
      await Promise.all(tasks);
      if (this.tab() === 'topology') await this.loadRouterInterfaces();
    } finally {
      this.loading.set(false);
    }
  }

  private async loadRouterInterfaces(): Promise<void> {
    const entries = await Promise.all(
      this.routers().map(async (r) => {
        try {
          const interfaces = await this.routerService.listInterfaces(r.id);
          return [r.externalId, interfaces.map((i) => i.subnetId).filter((s): s is string => !!s)] as const;
        } catch {
          return [r.externalId, []] as const;
        }
      }),
    );
    this.routerInterfaceSubnets.set(Object.fromEntries(entries));
  }
}
