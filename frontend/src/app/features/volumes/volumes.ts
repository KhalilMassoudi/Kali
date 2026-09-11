import { Component, computed, effect, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ButtonModule } from 'primeng/button';
import { TableModule } from 'primeng/table';
import { TagModule } from 'primeng/tag';
import { TooltipModule } from 'primeng/tooltip';
import { AuthService } from '../../core/services/auth.service';
import { VmService } from '../../core/services/vm.service';
import { VolumeService } from '../../core/services/volume.service';
import { ProjectService } from '../../core/services/project.service';
import { PreferencesService } from '../../core/services/preferences.service';
import { Volume, VolumeStatus } from '../../core/models/vm.model';
import { volumeStatusLabel, volumeStatusSeverity } from '../../shared/utils/vm-status.util';
import { CreateVolumeDialog } from './create-volume-dialog/create-volume-dialog';
import { AttachVolumeDialog } from './attach-volume-dialog/attach-volume-dialog';
import { VolumeSnapshotsDialog } from './volume-snapshots-dialog/volume-snapshots-dialog';
import { ProjectSelect } from '../../shared/project-select/project-select';

type FilterKey = 'all' | 'available' | 'in_use' | 'error';

const FILTERS: { key: FilterKey; label: string }[] = [
  { key: 'all', label: 'Tous' },
  { key: 'available', label: 'Disponibles' },
  { key: 'in_use', label: 'Attachés' },
  { key: 'error', label: 'En erreur' },
];

interface RowState {
  actionLoading: 'detach' | 'delete' | null;
}

@Component({
  selector: 'app-volumes',
  imports: [
    CommonModule,
    ButtonModule,
    TableModule,
    TagModule,
    TooltipModule,
    CreateVolumeDialog,
    AttachVolumeDialog,
    VolumeSnapshotsDialog,
    ProjectSelect,
  ],
  templateUrl: './volumes.html',
  styleUrl: './volumes.scss',
})
export class Volumes {
  private readonly auth = inject(AuthService);
  private readonly vmService = inject(VmService);
  readonly volumeService = inject(VolumeService);
  readonly projectService = inject(ProjectService);
  readonly preferences = inject(PreferencesService);

  readonly user = this.auth.user;
  readonly volumes = this.volumeService.volumes;
  readonly loading = this.volumeService.loading;
  readonly error = this.volumeService.error;
  readonly projects = this.projectService.projects;

  readonly filters = FILTERS;
  readonly filter = signal<FilterKey>('all');
  readonly showCreateDialog = signal(false);
  readonly showAttachDialog = signal(false);
  readonly attachTarget = signal<Volume | null>(null);
  readonly snapshotsTarget = signal<Volume | null>(null);

  readonly statusLabel = volumeStatusLabel;
  readonly statusSeverity = volumeStatusSeverity;

  private readonly rowStates = signal<Record<number, RowState>>({});

  readonly filtered = computed(() => {
    const key = this.filter();
    const list = this.volumes();
    if (key === 'all') return list;
    const target: VolumeStatus[] = key === 'available' ? ['AVAILABLE'] : key === 'in_use' ? ['IN_USE'] : ['ERROR'];
    return list.filter((v) => target.includes(v.status));
  });

  readonly counts = computed(() => {
    const list = this.volumes();
    return {
      all: list.length,
      available: list.filter((v) => v.status === 'AVAILABLE').length,
      in_use: list.filter((v) => v.status === 'IN_USE').length,
      error: list.filter((v) => v.status === 'ERROR').length,
    };
  });

  constructor() {
    effect(() => {
      const user = this.auth.user();
      if (user) {
        this.volumeService.fetchVolumes(user.id);
        this.vmService.fetchVms(user.id);
        this.projectService.fetchProjects(user.id);
      }
    });
  }

  async assignProject(volume: Volume, projectId: number | null): Promise<void> {
    await this.volumeService.assignProject(volume.id, projectId);
  }

  refresh(): void {
    const user = this.user();
    if (user) this.volumeService.fetchVolumes(user.id);
  }

  clearError(): void {
    this.volumeService.clearError();
  }

  vmName(vpsId: number | null): string {
    if (!vpsId) return '—';
    return this.vmService.vms().find((v) => v.id === vpsId)?.name ?? `VM #${vpsId}`;
  }

  rowState(volume: Volume): RowState {
    return this.rowStates()[volume.id] ?? { actionLoading: null };
  }

  private patchRow(id: number, patch: Partial<RowState>): void {
    this.rowStates.update((states) => ({ ...states, [id]: { ...(states[id] ?? { actionLoading: null }), ...patch } }));
  }

  openAttach(volume: Volume): void {
    this.attachTarget.set(volume);
    this.showAttachDialog.set(true);
  }

  openSnapshots(volume: Volume): void {
    this.snapshotsTarget.set(volume);
  }

  async detach(volume: Volume): Promise<void> {
    this.patchRow(volume.id, { actionLoading: 'detach' });
    try {
      await this.volumeService.detachVolume(volume.id);
    } finally {
      this.patchRow(volume.id, { actionLoading: null });
    }
  }

  async remove(volume: Volume): Promise<void> {
    this.patchRow(volume.id, { actionLoading: 'delete' });
    try {
      await this.volumeService.deleteVolume(volume.id);
    } finally {
      this.patchRow(volume.id, { actionLoading: null });
    }
  }

  emptyStateLabel(): string {
    if (this.filter() === 'all') return 'Aucun volume pour le moment';
    const f = this.filters.find((f) => f.key === this.filter());
    return `Aucun volume ${f?.label.toLowerCase()}`;
  }
}
