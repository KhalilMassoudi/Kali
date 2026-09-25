import { Component, computed, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { ButtonModule } from 'primeng/button';
import { SelectModule } from 'primeng/select';
import { AdminService } from '../../../core/services/admin.service';
import { MetricsService } from '../../../core/services/metrics.service';
import { TelemetryService } from '../../../core/services/telemetry.service';
import { MeasurePoint } from '../../../core/models/telemetry.model';
import { QuotaMeter } from '../../../core/models/vm.model';
import { GaugeGroup, gauge as makeGauge } from '../../../shared/resource-gauge/gauge-spec';
import { formatRam } from '../../../shared/utils/vm-status.util';
import { cumulativeCpuToUtilPercent } from '../../../shared/utils/telemetry.util';
import { TimeseriesChart } from '../../../shared/timeseries-chart/timeseries-chart';
import { ResourceGauge } from '../../../shared/resource-gauge/resource-gauge';

interface Shortcut {
  to: string;
  icon: string;
  label: string;
  count: () => number;
}

const gauge = (label: string, q: QuotaMeter | { used: number; limit: null }, unit = '', divisor = 1) =>
  makeGauge(label, q.used, q.limit, unit, divisor);

@Component({
  selector: 'app-admin-overview',
  imports: [CommonModule, FormsModule, RouterLink, ButtonModule, SelectModule, TimeseriesChart, ResourceGauge],
  templateUrl: './admin-overview.html',
  styleUrl: './admin-overview.scss',
})
export class AdminOverview {
  readonly admin = inject(AdminService);
  readonly metricsService = inject(MetricsService);
  private readonly telemetryService = inject(TelemetryService);

  readonly fleetMetrics = this.metricsService.fleetSummary;
  readonly platform = this.metricsService.platformTotals;
  readonly platformLoading = this.metricsService.platformLoading;

  /** Same layout as Horizon's "Limit Summary": usage vs. project quota, grouped by service. */
  readonly platformGroups = computed<GaugeGroup[]>(() => {
    const p = this.platform();
    if (p) {
      return [
        { title: 'Compute', gauges: [
          gauge('Instances', p.instances),
          gauge('vCPUs', p.vcpus),
          gauge('RAM', p.ramMb, ' GB', 1024),
        ] },
        { title: 'Volume', gauges: [
          gauge('Volumes', p.volumes),
          gauge('Snapshots', p.snapshots),
          gauge('Stockage volumes', p.volumeGb, ' GB'),
        ] },
        { title: 'Réseau', gauges: [
          gauge('IP flottantes', p.floatingIps),
          gauge('Groupes de sécurité', p.securityGroups),
          gauge('Règles de sécurité', p.securityGroupRules),
          gauge('Réseaux', p.networks),
          gauge('Ports', p.ports),
          gauge('Routeurs', p.routers),
        ] },
      ];
    }
    // OpenStack unreachable: fall back to our own tracking tables (no quotas known).
    const m = this.fleetMetrics();
    const count = (used: number) => ({ used, limit: null });
    return [
      { title: 'Compute', gauges: [
        gauge('Instances', count(this.admin.allVps().length)),
        gauge('vCPUs', count(m?.totalVcpu ?? 0)),
        gauge('RAM', count(m?.totalRamMb ?? 0), ' GB', 1024),
      ] },
      { title: 'Volume', gauges: [
        gauge('Volumes', count(this.admin.allVolumes().length)),
        gauge('Stockage VMs', count(m?.totalStorageGb ?? 0), ' GB'),
      ] },
      { title: 'Réseau', gauges: [
        gauge('Réseaux', count(this.admin.allNetworks().length)),
        gauge('Groupes de sécurité', count(this.admin.allSecurityGroups().length)),
      ] },
    ];
  });
  readonly formatRam = formatRam;

  readonly chartVmId = signal<number | null>(null);
  readonly chartLoading = signal(false);
  readonly chartUnavailable = signal(false);
  readonly cpuMeasures = signal<MeasurePoint[]>([]);

  readonly chartVmOptions = () =>
    this.admin.allVps()
      .filter((v) => v.externalId)
      .map((v) => ({ label: v.name, value: v.id }));

  readonly shortcuts: Shortcut[] = [
    { to: '/admin/users', icon: 'pi pi-users', label: 'Clients', count: () => this.admin.users().length },
    { to: '/admin/compute', icon: 'pi pi-server', label: 'Compute', count: () => this.admin.allVps().length },
    { to: '/admin/volumes', icon: 'pi pi-database', label: 'Volumes', count: () => this.admin.allVolumes().length },
    { to: '/admin/network', icon: 'pi pi-share-alt', label: 'Réseau', count: () => this.admin.allNetworks().length },
  ];

  /** Télémétrie has no standalone nav item anymore — reachable from here instead. */
  readonly telemetryLink = { to: '/admin/telemetry', icon: 'pi pi-chart-line', label: 'Télémétrie (Gnocchi)' };

  constructor() {
    this.load();
  }

  async refresh(): Promise<void> {
    await this.load();
  }

  onChartVmChange(vmId: number): void {
    this.chartVmId.set(vmId);
    this.loadChart();
  }

  private async load(): Promise<void> {
    await Promise.all([
      this.admin.loadUsers(),
      this.admin.loadAllVps(),
      this.admin.loadAllVolumes(),
      this.admin.loadAllNetworks(),
      this.admin.loadAllSecurityGroups(),
      this.metricsService.loadFleetSummary(),
      this.metricsService.loadPlatformTotals(),
    ]);
    // Right after an infrastructure-service restart the first OpenStack call can fail;
    // retry once quietly instead of leaving the fallback warning up until a manual refresh.
    if (!this.platform()) {
      setTimeout(() => this.metricsService.loadPlatformTotals(), 5000);
    }
    if (this.chartVmId() === null) {
      const first = this.chartVmOptions()[0];
      if (first) {
        this.chartVmId.set(first.value);
        this.loadChart();
      }
    }
  }

  private async loadChart(): Promise<void> {
    const vm = this.admin.allVps().find((v) => v.id === this.chartVmId());
    if (!vm?.externalId) {
      this.chartUnavailable.set(true);
      return;
    }
    this.chartLoading.set(true);
    this.chartUnavailable.set(false);
    try {
      const raw = await this.telemetryService.getMeasures(vm.externalId, 'cpu', 'instance', 300);
      const points = cumulativeCpuToUtilPercent(raw ?? [], vm.cpu ?? 1);
      this.cpuMeasures.set(points);
      this.chartUnavailable.set(points.length === 0);
    } catch {
      this.cpuMeasures.set([]);
      this.chartUnavailable.set(true);
    } finally {
      this.chartLoading.set(false);
    }
  }
}
