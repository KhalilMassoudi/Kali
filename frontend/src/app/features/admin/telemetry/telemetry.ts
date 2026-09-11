import { Component, computed, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ButtonModule } from 'primeng/button';
import { TableModule } from 'primeng/table';
import { SelectModule } from 'primeng/select';
import { TelemetryService } from '../../../core/services/telemetry.service';
import { AdminService } from '../../../core/services/admin.service';
import { MeasurePoint, TelemetryResource } from '../../../core/models/telemetry.model';
import { TimeseriesChart } from '../../../shared/timeseries-chart/timeseries-chart';

const GRANULARITY_OPTIONS = [
  { label: '1 min', value: 60 },
  { label: '5 min', value: 300 },
  { label: '1 heure', value: 3600 },
];

/** Friendlier labels for Gnocchi's raw resource "type" strings. */
const TYPE_LABELS: Record<string, string> = {
  instance: 'Instances',
  volume: 'Volumes',
  network: 'Réseaux',
  instance_disk: 'Disques instance',
  instance_network_interface: "Interfaces réseau",
  volume_provider: 'Fournisseurs de volumes',
  volume_provider_pool: 'Pools de volumes',
  image: 'Images',
};

@Component({
  selector: 'app-admin-telemetry',
  imports: [CommonModule, FormsModule, ButtonModule, TableModule, SelectModule, TimeseriesChart],
  templateUrl: './telemetry.html',
  styleUrl: './telemetry.scss',
})
export class AdminTelemetry {
  readonly telemetry = inject(TelemetryService);
  readonly admin = inject(AdminService);

  readonly loading = signal(true);
  readonly configured = this.telemetry.configured;
  readonly resources = this.telemetry.resources;
  readonly error = this.telemetry.error;

  readonly granularityOptions = GRANULARITY_OPTIONS;
  readonly granularity = signal(300);

  readonly selectedResource = signal<TelemetryResource | null>(null);
  readonly selectedMetric = signal<string | null>(null);

  readonly measures = signal<MeasurePoint[]>([]);
  readonly measuresLoading = signal(false);
  readonly measuresError = signal<string | null>(null);

  readonly selectedType = signal<string>('all');

  readonly typeGroups = computed(() => {
    const counts = new Map<string, number>();
    for (const r of this.resources()) counts.set(r.type, (counts.get(r.type) ?? 0) + 1);
    return [...counts.entries()]
      .map(([type, count]) => ({ type, label: TYPE_LABELS[type] ?? type, count }))
      .sort((a, b) => b.count - a.count);
  });

  readonly filteredResources = computed(() => {
    const type = this.selectedType();
    const list = this.resources();
    return type === 'all' ? list : list.filter((r) => r.type === type);
  });

  readonly metricNames = computed(() => {
    const resource = this.selectedResource();
    return resource ? Object.keys(resource.metrics ?? {}) : [];
  });

  constructor() {
    this.load();
  }

  private async load(): Promise<void> {
    this.loading.set(true);
    try {
      await Promise.all([
        this.telemetry.loadResources(),
        this.admin.loadAllVps(),
        this.admin.loadAllVolumes(),
        this.admin.loadAllNetworks(),
      ]);
    } finally {
      this.loading.set(false);
    }
  }

  selectType(type: string): void {
    this.selectedType.set(type);
  }

  /**
   * Gnocchi resources carry no human name of their own — this cross-references the resource
   * id (== the OpenStack UUID) against what our own platform already knows (VM/volume/network
   * name), falling back to a short label when the resource isn't one we track (e.g. an orphan
   * or another client's raw OpenStack resource).
   */
  resourceName(resource: TelemetryResource): string {
    const id = resource.id;
    // instance_disk/instance_network_interface don't use the VM's own UUID as their id —
    // it's embedded as a prefix of original_resource_id instead (e.g. "<vm-uuid>-vda").
    const originalId = typeof resource['original_resource_id'] === 'string' ? resource['original_resource_id'] : id;

    switch (resource.type) {
      case 'instance': {
        const vm = this.admin.allVps().find((v) => v.externalId === id);
        if (vm) return vm.name;
        break;
      }
      case 'instance_disk':
      case 'instance_network_interface': {
        const vm = this.admin.allVps().find((v) => v.externalId && originalId.startsWith(v.externalId));
        if (vm) return `${vm.name} (${TYPE_LABELS[resource.type] ?? resource.type})`;
        break;
      }
      case 'volume': {
        const vol = this.admin.allVolumes().find((v) => v.externalId === id);
        if (vol) return vol.name;
        break;
      }
      case 'network': {
        const net = this.admin.allNetworks().find((n) => n.externalId === id);
        if (net) return net.name;
        break;
      }
    }
    return `${TYPE_LABELS[resource.type] ?? resource.type} — ${id.slice(0, 8)}…`;
  }

  selectResource(resource: TelemetryResource): void {
    this.selectedResource.set(resource);
    this.selectedMetric.set(null);
    this.measures.set([]);
    this.measuresError.set(null);
    const firstMetric = Object.keys(resource.metrics ?? {})[0];
    if (firstMetric) this.selectMetric(firstMetric);
  }

  async selectMetric(metricName: string): Promise<void> {
    const resource = this.selectedResource();
    if (!resource) return;
    this.selectedMetric.set(metricName);
    await this.fetchMeasures();
  }

  async onGranularityChange(): Promise<void> {
    await this.fetchMeasures();
  }

  private async fetchMeasures(): Promise<void> {
    const resource = this.selectedResource();
    const metric = this.selectedMetric();
    if (!resource || !metric) return;

    this.measuresLoading.set(true);
    this.measuresError.set(null);
    try {
      const points = await this.telemetry.getMeasures(resource.id, metric, resource.type, this.granularity());
      this.measures.set(points ?? []);
    } catch {
      this.measuresError.set('Erreur lors du chargement des mesures');
      this.measures.set([]);
    } finally {
      this.measuresLoading.set(false);
    }
  }

  async refresh(): Promise<void> {
    await this.load();
  }
}
