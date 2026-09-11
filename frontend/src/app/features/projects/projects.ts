import { Component, effect, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ButtonModule } from 'primeng/button';
import { TableModule } from 'primeng/table';
import { TooltipModule } from 'primeng/tooltip';
import { AuthService } from '../../core/services/auth.service';
import { ProjectService } from '../../core/services/project.service';
import { ClientProject } from '../../core/models/project.model';
import { CreateProjectDialog } from './create-project-dialog/create-project-dialog';

interface RowState {
  deleting: boolean;
}

@Component({
  selector: 'app-projects',
  imports: [CommonModule, ButtonModule, TableModule, TooltipModule, CreateProjectDialog],
  templateUrl: './projects.html',
  styleUrl: './projects.scss',
})
export class Projects {
  private readonly auth = inject(AuthService);
  readonly projectService = inject(ProjectService);

  readonly user = this.auth.user;
  readonly projects = this.projectService.projects;
  readonly loading = this.projectService.loading;
  readonly error = this.projectService.error;

  readonly showCreateDialog = signal(false);
  private readonly rowStates = signal<Record<number, RowState>>({});

  constructor() {
    effect(() => {
      const user = this.auth.user();
      if (user) this.projectService.fetchProjects(user.id);
    });
  }

  refresh(): void {
    const user = this.user();
    if (user) this.projectService.fetchProjects(user.id);
  }

  clearError(): void {
    this.projectService.clearError();
  }

  rowState(project: ClientProject): RowState {
    return this.rowStates()[project.id] ?? { deleting: false };
  }

  async remove(project: ClientProject): Promise<void> {
    this.rowStates.update((s) => ({ ...s, [project.id]: { deleting: true } }));
    try {
      await this.projectService.deleteProject(project.id);
    } finally {
      this.rowStates.update((s) => ({ ...s, [project.id]: { deleting: false } }));
    }
  }
}
