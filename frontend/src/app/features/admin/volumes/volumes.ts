import { Component, computed, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { ButtonModule } from 'primeng/button';
import { TableModule } from 'primeng/table';
import { TagModule } from 'primeng/tag';
import { SelectModule } from 'primeng/select';
import { AdminService } from '../../../core/services/admin.service';
import { VolumeGroupService } from '../../../core/services/volume-group.service';
import { CreateVolumeDialog } from '../../volumes/create-volume-dialog/create-volume-dialog';

type Tab = 'volumes' | 'snapshots' | 'groups';

@Component({
  selector: 'app-admin-volumes',
  imports: [CommonModule, FormsModule, ButtonModule, TableModule, TagModule, SelectModule, CreateVolumeDialog],
  templateUrl: './volumes.html',
  styleUrl: './volumes.scss',
})
export class AdminVolumes {
  readonly admin = inject(AdminService);
  readonly volumeGroupService = inject(VolumeGroupService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);

  readonly tab = signal<Tab>('volumes');
  readonly loading = signal(true);
  readonly error = this.admin.error;

  readonly volumes = this.admin.allVolumes;
  readonly snapshots = this.admin.allVolumeSnapshots;
  readonly groupsSupported = this.volumeGroupService.supported;

  readonly targetUserId = signal<number | null>(null);
  readonly clientOptions = computed(() =>
    this.admin.users().map((u) => ({ label: `${u.email}${u.firstName ? ' — ' + u.firstName : ''}`, value: u.id })),
  );
  readonly showCreateVolumeDialog = signal(false);

  constructor() {
    const initial = this.route.snapshot.queryParamMap.get('tab') as Tab | null;
    if (initial) this.tab.set(initial);
    if (this.admin.users().length === 0) this.admin.loadUsers();
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
        this.admin.loadAllVolumes(),
        this.admin.loadAllVolumeSnapshots(),
        this.volumeGroupService.checkSupported(),
      ]);
    } finally {
      this.loading.set(false);
    }
  }
}
