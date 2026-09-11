import { Component, effect, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ButtonModule } from 'primeng/button';
import { TableModule } from 'primeng/table';
import { TagModule } from 'primeng/tag';
import { TooltipModule } from 'primeng/tooltip';
import { AuthService } from '../../core/services/auth.service';
import { NetworkService } from '../../core/services/network.service';
import { ProjectService } from '../../core/services/project.service';
import { VpsNetwork } from '../../core/models/vm.model';
import { CreateNetworkDialog } from './create-network-dialog/create-network-dialog';
import { ProjectSelect } from '../../shared/project-select/project-select';

interface RowState {
  deleting: boolean;
}

@Component({
  selector: 'app-networks',
  imports: [CommonModule, ButtonModule, TableModule, TagModule, TooltipModule, CreateNetworkDialog, ProjectSelect],
  templateUrl: './networks.html',
  styleUrl: './networks.scss',
})
export class Networks {
  private readonly auth = inject(AuthService);
  readonly networkService = inject(NetworkService);
  readonly projectService = inject(ProjectService);

  readonly user = this.auth.user;
  readonly networks = this.networkService.networks;
  readonly loading = this.networkService.loading;
  readonly error = this.networkService.error;
  readonly projects = this.projectService.projects;

  readonly showCreateDialog = signal(false);
  private readonly rowStates = signal<Record<number, RowState>>({});

  constructor() {
    effect(() => {
      const user = this.auth.user();
      if (user) {
        this.networkService.fetchNetworks(user.id);
        this.projectService.fetchProjects(user.id);
      }
    });
  }

  async assignProject(network: VpsNetwork, projectId: number | null): Promise<void> {
    await this.networkService.assignProject(network.id, projectId);
  }

  refresh(): void {
    const user = this.user();
    if (user) this.networkService.fetchNetworks(user.id);
  }

  clearError(): void {
    this.networkService.clearError();
  }

  rowState(network: VpsNetwork): RowState {
    return this.rowStates()[network.id] ?? { deleting: false };
  }

  async remove(network: VpsNetwork): Promise<void> {
    this.rowStates.update((s) => ({ ...s, [network.id]: { deleting: true } }));
    try {
      await this.networkService.deleteNetwork(network.id);
    } finally {
      this.rowStates.update((s) => ({ ...s, [network.id]: { deleting: false } }));
    }
  }
}
