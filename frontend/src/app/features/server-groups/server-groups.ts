import { Component, effect, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ButtonModule } from 'primeng/button';
import { TableModule } from 'primeng/table';
import { TagModule } from 'primeng/tag';
import { TooltipModule } from 'primeng/tooltip';
import { AuthService } from '../../core/services/auth.service';
import { ServerGroupService } from '../../core/services/server-group.service';
import { ClientServerGroup } from '../../core/models/vm.model';
import { CreateServerGroupDialog } from './create-server-group-dialog/create-server-group-dialog';

interface RowState {
  deleting: boolean;
}

const POLICY_LABELS: Record<string, string> = {
  AFFINITY: 'Affinité',
  ANTI_AFFINITY: 'Anti-affinité',
  SOFT_AFFINITY: 'Affinité souple',
  SOFT_ANTI_AFFINITY: 'Anti-affinité souple',
};

@Component({
  selector: 'app-server-groups',
  imports: [CommonModule, ButtonModule, TableModule, TagModule, TooltipModule, CreateServerGroupDialog],
  templateUrl: './server-groups.html',
  styleUrl: './server-groups.scss',
})
export class ServerGroups {
  private readonly auth = inject(AuthService);
  readonly groupService = inject(ServerGroupService);

  readonly user = this.auth.user;
  readonly groups = this.groupService.groups;
  readonly loading = this.groupService.loading;
  readonly error = this.groupService.error;

  readonly showCreateDialog = signal(false);
  private readonly rowStates = signal<Record<number, RowState>>({});

  constructor() {
    effect(() => {
      const user = this.auth.user();
      if (user) this.groupService.fetchGroups(user.id);
    });
  }

  refresh(): void {
    const user = this.user();
    if (user) this.groupService.fetchGroups(user.id);
  }

  clearError(): void {
    this.groupService.clearError();
  }

  policyLabel(policy: string): string {
    return POLICY_LABELS[policy] ?? policy;
  }

  rowState(group: ClientServerGroup): RowState {
    return this.rowStates()[group.id] ?? { deleting: false };
  }

  async remove(group: ClientServerGroup): Promise<void> {
    this.rowStates.update((s) => ({ ...s, [group.id]: { deleting: true } }));
    try {
      await this.groupService.deleteGroup(group.id);
    } finally {
      this.rowStates.update((s) => ({ ...s, [group.id]: { deleting: false } }));
    }
  }
}
