import { Component, computed, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { ButtonModule } from 'primeng/button';
import { TagModule } from 'primeng/tag';
import { InputTextModule } from 'primeng/inputtext';
import { SelectModule } from 'primeng/select';
import { TooltipModule } from 'primeng/tooltip';
import { AuthService } from '../../../core/services/auth.service';
import { VmService } from '../../../core/services/vm.service';
import { VolumeService } from '../../../core/services/volume.service';
import { TelemetryService } from '../../../core/services/telemetry.service';
import { TimeseriesChart } from '../../../shared/timeseries-chart/timeseries-chart';
import {
  BackupFrequency,
  FlavorOption,
  ImageOption,
  InterfaceDetails,
  NetworkOption,
  SecurityGroupOption,
  Volume,
  VmMetadata,
  VpsActivityLog,
  VpsBackupSchedule,
} from '../../../core/models/vm.model';
import { MeasurePoint } from '../../../core/models/telemetry.model';
import { formatRam, vmStatusLabel, vmStatusSeverity } from '../../../shared/utils/vm-status.util';
import { cumulativeCpuToUtilPercent } from '../../../shared/utils/telemetry.util';

@Component({
  selector: 'app-vm-detail',
  imports: [
    CommonModule,
    RouterLink,
    FormsModule,
    ButtonModule,
    TagModule,
    InputTextModule,
    SelectModule,
    TooltipModule,
    TimeseriesChart,
  ],
  templateUrl: './vm-detail.html',
  styleUrl: './vm-detail.scss',
})
export class VmDetail {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly auth = inject(AuthService);
  readonly vmService = inject(VmService);
  readonly volumeService = inject(VolumeService);
  readonly telemetryService = inject(TelemetryService);

  readonly isAdmin = computed(() => this.auth.user()?.role === 'ADMIN');

  readonly vm = this.vmService.currentVm;
  readonly loading = this.vmService.loading;
  readonly error = this.vmService.error;

  readonly statusLabel = vmStatusLabel;
  readonly statusSeverity = vmStatusSeverity;
  readonly formatRam = formatRam;

  private readonly vmId = Number(this.route.snapshot.paramMap.get('id'));

  // Header / actions
  readonly actionLoading = signal<string | null>(null);
  readonly editingName = signal(false);
  readonly nameDraft = signal('');
  readonly confirmDelete = signal(false);

  // SSH
  readonly sshCopied = signal(false);
  readonly sshCommand = computed(() => {
    const v = this.vm();
    const ip = v?.floatingIp || v?.ipAddress;
    return ip ? `ssh root@${ip}` : null;
  });

  // Security groups
  readonly catalogGroups = signal<SecurityGroupOption[]>([]);
  readonly catalogNetworks = signal<NetworkOption[]>([]);
  readonly currentGroupNames = signal<Set<string>>(new Set());
  readonly sgActionLoading = signal<string | null>(null);

  // Volumes
  readonly attachedVolumes = computed(() => this.volumeService.volumes().filter((v) => v.attachedVpsId === this.vmId));
  readonly availableVolumes = computed(() => this.volumeService.volumes().filter((v) => v.attachedVpsId === null));
  readonly selectedVolumeId = signal<number | null>(null);
  readonly volumeActionLoading = signal<number | null>(null);

  // Resize
  readonly flavors = signal<FlavorOption[]>([]);
  readonly selectedFlavorId = signal<string | null>(null);
  readonly resizing = signal(false);

  // Snapshot
  readonly snapshotName = signal('');
  readonly snapshotting = signal(false);
  readonly snapshotResult = signal<string | null>(null);

  // Metadata
  readonly metadata = signal<VmMetadata>({});
  readonly newMetaKey = signal('');
  readonly newMetaValue = signal('');
  readonly metaActionLoading = signal<string | null>(null);

  // Utilization (Gnocchi)
  readonly cpuMeasures = signal<MeasurePoint[]>([]);
  readonly telemetryUnavailable = signal(false);

  // Activity log
  readonly activityLog = signal<VpsActivityLog[]>([]);
  readonly activityLoading = signal(false);

  // Backups / scheduled snapshots
  readonly backupSchedule = signal<VpsBackupSchedule | null>(null);
  readonly backups = signal<ImageOption[]>([]);
  readonly backupFrequency = signal<BackupFrequency>('DAILY');
  readonly backupRetention = signal(3);
  readonly backupEnabled = signal(true);
  readonly backupSaving = signal(false);

  // Rescue mode (admin-only)
  readonly rescuing = signal(false);
  readonly rescuePassword = signal<string | null>(null);

  // Advanced actions (pause/suspend/shelve/lock/soft reboot/rebuild)
  readonly advancedActionLoading = signal<string | null>(null);
  readonly rebuildImageId = signal('');
  readonly showRebuildForm = signal(false);

  // Console
  readonly consoleLoading = signal(false);
  readonly consoleLogLoading = signal(false);
  readonly consoleLog = signal<string | null>(null);
  readonly showConsoleLogDialog = signal(false);

  // Network interfaces
  readonly interfaces = signal<InterfaceDetails[]>([]);
  readonly interfacesLoading = signal(false);
  readonly selectedAttachNetworkId = signal<string | null>(null);
  readonly interfaceActionLoading = signal<string | null>(null);
  readonly portSgTarget = signal<InterfaceDetails | null>(null);
  readonly portSgSelected = signal<Set<string>>(new Set());
  readonly portSgSaving = signal(false);

  get flavorOptions() {
    return this.flavors().map((f) => ({ label: `${f.name} — ${f.vcpus} vCPU, ${f.ram}MB RAM, ${f.disk}GB disque`, value: f.id }));
  }

  get metadataEntries(): [string, string][] {
    return Object.entries(this.metadata());
  }

  constructor() {
    this.load();
  }

  private async load(): Promise<void> {
    try {
      await this.vmService.getVm(this.vmId);
    } catch {
      this.router.navigate(['/vms']);
      return;
    }
    const userId = this.auth.user()?.id;
    if (userId) this.volumeService.fetchVolumes(userId);
    this.loadSecurityGroups();
    this.loadMetadata();
    this.vmService.fetchFlavors().then((f) => this.flavors.set(f)).catch(() => {});
    this.loadUtilization();
    this.loadActivity();
    this.loadBackupSchedule();
    this.loadBackups();
    this.loadInterfaces();
    this.vmService.fetchNetworks().then((nets) => this.catalogNetworks.set(nets)).catch(() => {});
  }

  // ── Utilization (Gnocchi) ───────────────────────────────────────
  // This deployment's Ceilometer pipeline only publishes the raw cumulative "cpu" counter
  // (nanoseconds of CPU time since boot) — there's no pre-computed "cpu_util" percentage
  // metric anywhere in Gnocchi here (confirmed directly against the deployment). So the
  // chart requests "cpu" and derives a % utilization curve itself from consecutive samples.
  private async loadUtilization(): Promise<void> {
    const externalId = this.vm()?.externalId;
    if (!externalId) return;
    try {
      const raw = await this.telemetryService.getMeasures(externalId, 'cpu', 'instance', 300);
      const vcpus = this.vm()?.cpu ?? 1;
      const points = cumulativeCpuToUtilPercent(raw ?? [], vcpus);
      this.cpuMeasures.set(points);
      this.telemetryUnavailable.set(points.length === 0);
    } catch {
      this.telemetryUnavailable.set(true);
    }
  }

  // ── Activity log ─────────────────────────────────────────────────
  private async loadActivity(): Promise<void> {
    this.activityLoading.set(true);
    try {
      const page = await this.vmService.getActivity(this.vmId);
      this.activityLog.set(page.content);
    } catch {
      this.activityLog.set([]);
    } finally {
      this.activityLoading.set(false);
    }
  }

  // ── Backups ──────────────────────────────────────────────────────
  private async loadBackupSchedule(): Promise<void> {
    const schedule = await this.vmService.getBackupSchedule(this.vmId);
    this.backupSchedule.set(schedule);
    if (schedule) {
      this.backupFrequency.set(schedule.frequency);
      this.backupRetention.set(schedule.retentionCount);
      this.backupEnabled.set(schedule.enabled);
    }
  }

  private async loadBackups(): Promise<void> {
    try {
      this.backups.set(await this.vmService.listBackups(this.vmId));
    } catch {
      this.backups.set([]);
    }
  }

  async saveBackupSchedule(): Promise<void> {
    this.backupSaving.set(true);
    try {
      const schedule = await this.vmService.saveBackupSchedule(this.vmId, {
        frequency: this.backupFrequency(),
        retentionCount: this.backupRetention(),
        enabled: this.backupEnabled(),
      });
      this.backupSchedule.set(schedule);
    } finally {
      this.backupSaving.set(false);
    }
  }

  async deleteBackupSchedule(): Promise<void> {
    this.backupSaving.set(true);
    try {
      await this.vmService.deleteBackupSchedule(this.vmId);
      this.backupSchedule.set(null);
    } finally {
      this.backupSaving.set(false);
    }
  }

  // ── Rescue mode (admin-only) ────────────────────────────────────
  async rescue(): Promise<void> {
    this.rescuing.set(true);
    try {
      const password = await this.vmService.rescueVm(this.vmId);
      this.rescuePassword.set(password);
    } finally {
      this.rescuing.set(false);
    }
  }

  async unrescue(): Promise<void> {
    this.rescuing.set(true);
    try {
      await this.vmService.unrescueVm(this.vmId);
      this.rescuePassword.set(null);
    } finally {
      this.rescuing.set(false);
    }
  }

  // ── Advanced actions ────────────────────────────────────────────
  private async runAdvancedAction(key: string, action: () => Promise<void>): Promise<void> {
    this.advancedActionLoading.set(key);
    try {
      await action();
    } finally {
      this.advancedActionLoading.set(null);
    }
  }

  pause(): Promise<void> { return this.runAdvancedAction('pause', () => this.vmService.pauseVm(this.vmId)); }
  unpause(): Promise<void> { return this.runAdvancedAction('unpause', () => this.vmService.unpauseVm(this.vmId)); }
  suspend(): Promise<void> { return this.runAdvancedAction('suspend', () => this.vmService.suspendVm(this.vmId)); }
  resume(): Promise<void> { return this.runAdvancedAction('resume', () => this.vmService.resumeVm(this.vmId)); }
  shelve(): Promise<void> { return this.runAdvancedAction('shelve', () => this.vmService.shelveVm(this.vmId)); }
  unshelve(): Promise<void> { return this.runAdvancedAction('unshelve', () => this.vmService.unshelveVm(this.vmId)); }
  lockVm(): Promise<void> { return this.runAdvancedAction('lock', () => this.vmService.lockVm(this.vmId)); }
  unlockVm(): Promise<void> { return this.runAdvancedAction('unlock', () => this.vmService.unlockVm(this.vmId)); }
  softReboot(): Promise<void> { return this.runAdvancedAction('soft-reboot', () => this.vmService.softRebootVm(this.vmId)); }

  async rebuild(): Promise<void> {
    const imageId = this.rebuildImageId().trim();
    if (!imageId) return;
    this.advancedActionLoading.set('rebuild');
    try {
      await this.vmService.rebuildVm(this.vmId, imageId);
      this.rebuildImageId.set('');
      this.showRebuildForm.set(false);
    } finally {
      this.advancedActionLoading.set(null);
    }
  }

  // ── Console ──────────────────────────────────────────────────────
  async openConsole(): Promise<void> {
    this.consoleLoading.set(true);
    try {
      const url = await this.vmService.getConsoleUrl(this.vmId);
      if (url) window.open(url, '_blank', 'noopener');
    } finally {
      this.consoleLoading.set(false);
    }
  }

  async viewConsoleLog(): Promise<void> {
    this.consoleLogLoading.set(true);
    this.showConsoleLogDialog.set(true);
    try {
      this.consoleLog.set(await this.vmService.getConsoleLog(this.vmId, 200));
    } catch {
      this.consoleLog.set(null);
    } finally {
      this.consoleLogLoading.set(false);
    }
  }

  // ── Network interfaces ──────────────────────────────────────────
  private async loadInterfaces(): Promise<void> {
    this.interfacesLoading.set(true);
    try {
      this.interfaces.set(await this.vmService.listInterfaces(this.vmId));
    } catch {
      this.interfaces.set([]);
    } finally {
      this.interfacesLoading.set(false);
    }
  }

  networkName(networkId: string): string {
    return this.catalogNetworks().find((n) => n.id === networkId)?.name ?? networkId;
  }

  async attachInterface(): Promise<void> {
    const networkId = this.selectedAttachNetworkId();
    if (!networkId) return;
    this.interfaceActionLoading.set('attach');
    try {
      await this.vmService.attachInterface(this.vmId, networkId);
      this.selectedAttachNetworkId.set(null);
      await this.loadInterfaces();
    } finally {
      this.interfaceActionLoading.set(null);
    }
  }

  async detachInterface(attachmentId: string): Promise<void> {
    this.interfaceActionLoading.set(attachmentId);
    try {
      await this.vmService.detachInterface(this.vmId, attachmentId);
      await this.loadInterfaces();
    } finally {
      this.interfaceActionLoading.set(null);
    }
  }

  async openPortSecurityGroups(iface: InterfaceDetails): Promise<void> {
    this.portSgTarget.set(iface);
    try {
      const groups = await this.vmService.getPortSecurityGroups(this.vmId, iface.portId);
      this.portSgSelected.set(new Set(groups));
    } catch {
      this.portSgSelected.set(new Set());
    }
  }

  closePortSecurityGroups(): void {
    this.portSgTarget.set(null);
  }

  togglePortSecurityGroup(groupId: string): void {
    this.portSgSelected.update((current) => {
      const next = new Set(current);
      if (next.has(groupId)) next.delete(groupId);
      else next.add(groupId);
      return next;
    });
  }

  async savePortSecurityGroups(): Promise<void> {
    const iface = this.portSgTarget();
    if (!iface) return;
    this.portSgSaving.set(true);
    try {
      await this.vmService.updatePortSecurityGroups(this.vmId, iface.portId, [...this.portSgSelected()]);
      this.closePortSecurityGroups();
    } finally {
      this.portSgSaving.set(false);
    }
  }

  private async loadSecurityGroups(): Promise<void> {
    try {
      const [catalog, current] = await Promise.all([
        this.vmService.fetchSecurityGroups(),
        this.vmService.getCurrentSecurityGroups(this.vmId),
      ]);
      this.catalogGroups.set(catalog);
      this.currentGroupNames.set(new Set(current.map((g) => g.name)));
    } catch {
      // Non-critical — section just shows nothing assigned.
    }
  }

  private async loadMetadata(): Promise<void> {
    try {
      this.metadata.set(await this.vmService.getMetadata(this.vmId));
    } catch {
      // Non-critical
    }
  }

  // ── Header actions ──────────────────────────────────────────────
  startEditName(): void {
    this.nameDraft.set(this.vm()?.name ?? '');
    this.editingName.set(true);
  }

  cancelEditName(): void {
    this.editingName.set(false);
  }

  async saveName(): Promise<void> {
    const name = this.nameDraft().trim();
    if (!name) return;
    this.actionLoading.set('rename');
    try {
      await this.vmService.renameVm(this.vmId, name);
      this.editingName.set(false);
    } finally {
      this.actionLoading.set(null);
    }
  }

  async start(): Promise<void> {
    this.actionLoading.set('start');
    try { await this.vmService.startVm(this.vmId); } finally { this.actionLoading.set(null); }
  }

  async stop(): Promise<void> {
    this.actionLoading.set('stop');
    try { await this.vmService.stopVm(this.vmId); } finally { this.actionLoading.set(null); }
  }

  async restart(): Promise<void> {
    this.actionLoading.set('restart');
    try { await this.vmService.restartVm(this.vmId); } finally { this.actionLoading.set(null); }
  }

  async refreshStatus(): Promise<void> {
    this.actionLoading.set('refresh');
    try { await this.vmService.refreshVmStatus(this.vmId); } finally { this.actionLoading.set(null); }
  }

  async deleteVm(): Promise<void> {
    this.actionLoading.set('delete');
    try {
      await this.vmService.deleteVm(this.vmId);
      this.router.navigate(['/vms']);
    } finally {
      this.actionLoading.set(null);
    }
  }

  // ── SSH ──────────────────────────────────────────────────────────
  async copySsh(): Promise<void> {
    const cmd = this.sshCommand();
    if (!cmd) return;
    await navigator.clipboard.writeText(cmd);
    this.sshCopied.set(true);
    setTimeout(() => this.sshCopied.set(false), 2000);
  }

  async allocateFloatingIp(): Promise<void> {
    this.actionLoading.set('floating-ip');
    try { await this.vmService.allocateFloatingIp(this.vmId); } finally { this.actionLoading.set(null); }
  }

  async releaseFloatingIp(): Promise<void> {
    this.actionLoading.set('floating-ip');
    try { await this.vmService.releaseFloatingIp(this.vmId); } finally { this.actionLoading.set(null); }
  }

  // ── Security groups ─────────────────────────────────────────────
  isAssigned(name: string): boolean {
    return this.currentGroupNames().has(name);
  }

  async toggleGroup(group: SecurityGroupOption): Promise<void> {
    this.sgActionLoading.set(group.name);
    try {
      if (this.isAssigned(group.name)) {
        await this.vmService.removeSecurityGroup(this.vmId, group.name);
        this.currentGroupNames.update((s) => { const next = new Set(s); next.delete(group.name); return next; });
      } else {
        await this.vmService.assignSecurityGroup(this.vmId, group.name);
        this.currentGroupNames.update((s) => new Set(s).add(group.name));
      }
    } finally {
      this.sgActionLoading.set(null);
    }
  }

  // ── Volumes ──────────────────────────────────────────────────────
  async attachVolume(): Promise<void> {
    const volId = this.selectedVolumeId();
    if (!volId) return;
    this.volumeActionLoading.set(volId);
    try {
      await this.volumeService.attachVolume(volId, this.vmId);
      this.selectedVolumeId.set(null);
    } finally {
      this.volumeActionLoading.set(null);
    }
  }

  async detachVolume(volume: Volume): Promise<void> {
    this.volumeActionLoading.set(volume.id);
    try {
      await this.volumeService.detachVolume(volume.id);
    } finally {
      this.volumeActionLoading.set(null);
    }
  }

  // ── Resize ───────────────────────────────────────────────────────
  async resize(): Promise<void> {
    const flavorId = this.selectedFlavorId();
    if (!flavorId) return;
    this.resizing.set(true);
    try {
      await this.vmService.resizeVm(this.vmId, flavorId);
    } finally {
      this.resizing.set(false);
    }
  }

  async confirmResize(): Promise<void> {
    this.resizing.set(true);
    try { await this.vmService.confirmResize(this.vmId); } finally { this.resizing.set(false); }
  }

  async revertResize(): Promise<void> {
    this.resizing.set(true);
    try { await this.vmService.revertResize(this.vmId); } finally { this.resizing.set(false); }
  }

  // ── Snapshot ─────────────────────────────────────────────────────
  async takeSnapshot(): Promise<void> {
    const name = this.snapshotName().trim();
    if (!name) return;
    this.snapshotting.set(true);
    this.snapshotResult.set(null);
    try {
      const imageId = await this.vmService.snapshotVm(this.vmId, name);
      this.snapshotResult.set(imageId);
      this.snapshotName.set('');
    } finally {
      this.snapshotting.set(false);
    }
  }

  // ── Metadata ─────────────────────────────────────────────────────
  async addMetadata(): Promise<void> {
    const key = this.newMetaKey().trim();
    const value = this.newMetaValue().trim();
    if (!key) return;
    this.metaActionLoading.set(key);
    try {
      const updated = await this.vmService.updateMetadata(this.vmId, { ...this.metadata(), [key]: value });
      this.metadata.set(updated);
      this.newMetaKey.set('');
      this.newMetaValue.set('');
    } finally {
      this.metaActionLoading.set(null);
    }
  }

  async deleteMetadata(key: string): Promise<void> {
    this.metaActionLoading.set(key);
    try {
      await this.vmService.deleteMetadataKey(this.vmId, key);
      this.metadata.update((m) => { const next = { ...m }; delete next[key]; return next; });
    } finally {
      this.metaActionLoading.set(null);
    }
  }
}
