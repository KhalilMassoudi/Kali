import { Component, computed, effect, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterLink } from '@angular/router';
import { ButtonModule } from 'primeng/button';
import { AuthService } from '../../core/services/auth.service';
import { VmService } from '../../core/services/vm.service';
import { QuotaService } from '../../core/services/quota.service';
import { MetricsService } from '../../core/services/metrics.service';
import { VolumeService } from '../../core/services/volume.service';
import { NetworkService } from '../../core/services/network.service';
import { SecurityGroupService } from '../../core/services/security-group.service';
import { VmTable } from '../../shared/vm-table/vm-table';
import { AdminOverview } from './admin-overview/admin-overview';
import { ResourceGauge } from '../../shared/resource-gauge/resource-gauge';
import { GaugeGroup, gauge } from '../../shared/resource-gauge/gauge-spec';

interface QuickAction {
  icon: string;
  title: string;
  desc: string;
  color: string;
  action: () => void;
}

@Component({
  selector: 'app-dashboard',
  imports: [CommonModule, RouterLink, ButtonModule, VmTable, AdminOverview, ResourceGauge],
  templateUrl: './dashboard.html',
  styleUrl: './dashboard.scss',
})
export class Dashboard {
  private readonly auth = inject(AuthService);
  readonly vmService = inject(VmService);
  readonly quotaService = inject(QuotaService);
  readonly metricsService = inject(MetricsService);
  readonly volumeService = inject(VolumeService);
  readonly networkService = inject(NetworkService);
  readonly securityGroupService = inject(SecurityGroupService);
  private readonly router = inject(Router);

  readonly quota = this.quotaService.myQuota;

  /** Same grouped used/limit layout as the admin overview, scoped to this client's own quota. */
  readonly quotaGroups = computed<GaugeGroup[]>(() => {
    const q = this.quota();
    if (!q) return [];
    return [
      { title: 'Compute', gauges: [
        gauge('Instances', q.usage.vmCount, q.quota.maxVms),
        gauge('vCPUs', q.usage.totalVcpu, q.quota.maxVcpu),
        gauge('RAM', q.usage.totalRamMb, q.quota.maxRamMb, ' GB', 1024),
      ] },
      { title: 'Stockage', gauges: [
        gauge('Volumes', q.usage.volumeCount, q.quota.maxVolumes),
        gauge('Stockage total', q.usage.totalStorageGb, q.quota.maxStorageGb, ' GB'),
      ] },
      { title: 'Réseau', gauges: [
        gauge('Réseaux', q.usage.networkCount, q.quota.maxNetworks),
        gauge('Groupes de sécurité', q.usage.securityGroupCount, q.quota.maxSecurityGroups),
      ] },
    ];
  });
  readonly metrics = this.metricsService.mySummary;

  readonly isAdmin = computed(() => this.auth.user()?.role === 'ADMIN');
  readonly user = this.auth.user;
  readonly vms = this.vmService.vms;
  readonly loading = this.vmService.loading;
  readonly running = this.vmService.running;
  readonly stopped = this.vmService.stopped;
  readonly errored = this.vmService.errored;
  readonly totalCpu = this.vmService.totalCpu;
  readonly totalRam = this.vmService.totalRam;
  readonly volumes = this.volumeService.volumes;
  readonly networks = this.networkService.networks;
  readonly securityGroups = this.securityGroupService.groups;

  readonly recentVms = computed(() => this.vms().slice(0, 6));

  readonly firstName = computed(() => this.user()?.firstName || this.user()?.email?.split('@')[0] || 'là');

  readonly ramDisplay = computed(() => {
    const v = this.totalRam();
    return v >= 1024 ? `${(v / 1024).toFixed(1)} GB` : `${v} MB`;
  });

  readonly quickActions: QuickAction[] = [
    {
      icon: 'pi pi-plus',
      title: 'Créer une VM',
      desc: 'Déployer un nouveau serveur',
      color: 'accent',
      action: () => this.router.navigate(['/vms']),
    },
    {
      icon: 'pi pi-comments',
      title: 'Assistant IA',
      desc: 'Support en langage naturel',
      color: 'accent',
      action: () => this.router.navigate(['/chat']),
    },
    {
      icon: 'pi pi-chart-line',
      title: 'Monitoring',
      desc: 'Surveiller les performances',
      color: 'good',
      action: () => this.router.navigate(['/dashboard']),
    },
  ];

  constructor() {
    effect(() => {
      const user = this.auth.user();
      if (user) {
        this.vmService.fetchVms(user.id);
        this.volumeService.fetchVolumes(user.id);
        this.networkService.fetchNetworks(user.id);
        this.securityGroupService.fetchGroups(user.id);
      }
    });
    this.quotaService.loadMyQuota();
    this.metricsService.loadMySummary();
  }

  refresh(): void {
    const user = this.user();
    if (user) {
      this.vmService.fetchVms(user.id);
      this.volumeService.fetchVolumes(user.id);
      this.networkService.fetchNetworks(user.id);
      this.securityGroupService.fetchGroups(user.id);
    }
    this.quotaService.loadMyQuota();
    this.metricsService.loadMySummary();
  }

  pad(n: number): string {
    return String(n).padStart(2, '0');
  }
}
