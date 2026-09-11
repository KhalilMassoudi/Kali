import { Component, effect, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ButtonModule } from 'primeng/button';
import { TableModule } from 'primeng/table';
import { TagModule } from 'primeng/tag';
import { TooltipModule } from 'primeng/tooltip';
import { AuthService } from '../../core/services/auth.service';
import { SecurityGroupService } from '../../core/services/security-group.service';
import { ProjectService } from '../../core/services/project.service';
import { VpsSecurityGroup } from '../../core/models/vm.model';
import { CreateSecurityGroupDialog } from './create-security-group-dialog/create-security-group-dialog';
import { SecurityGroupRulesDialog } from './security-group-rules-dialog/security-group-rules-dialog';
import { ProjectSelect } from '../../shared/project-select/project-select';

interface RowState {
  deleting: boolean;
}

@Component({
  selector: 'app-security-groups',
  imports: [
    CommonModule,
    ButtonModule,
    TableModule,
    TagModule,
    TooltipModule,
    CreateSecurityGroupDialog,
    SecurityGroupRulesDialog,
    ProjectSelect,
  ],
  templateUrl: './security-groups.html',
  styleUrl: './security-groups.scss',
})
export class SecurityGroups {
  private readonly auth = inject(AuthService);
  readonly groupService = inject(SecurityGroupService);
  readonly projectService = inject(ProjectService);

  readonly user = this.auth.user;
  readonly groups = this.groupService.groups;
  readonly loading = this.groupService.loading;
  readonly error = this.groupService.error;
  readonly projects = this.projectService.projects;

  readonly showCreateDialog = signal(false);
  readonly rulesTarget = signal<VpsSecurityGroup | null>(null);
  private readonly rowStates = signal<Record<number, RowState>>({});

  constructor() {
    effect(() => {
      const user = this.auth.user();
      if (user) {
        this.groupService.fetchGroups(user.id);
        this.projectService.fetchProjects(user.id);
      }
    });
  }

  async assignProject(group: VpsSecurityGroup, projectId: number | null): Promise<void> {
    await this.groupService.assignProject(group.id, projectId);
  }

  refresh(): void {
    const user = this.user();
    if (user) this.groupService.fetchGroups(user.id);
  }

  clearError(): void {
    this.groupService.clearError();
  }

  rowState(group: VpsSecurityGroup): RowState {
    return this.rowStates()[group.id] ?? { deleting: false };
  }

  manageRules(group: VpsSecurityGroup): void {
    this.rulesTarget.set(group);
  }

  async remove(group: VpsSecurityGroup): Promise<void> {
    this.rowStates.update((s) => ({ ...s, [group.id]: { deleting: true } }));
    try {
      await this.groupService.deleteGroup(group.id);
    } finally {
      this.rowStates.update((s) => ({ ...s, [group.id]: { deleting: false } }));
    }
  }
}
