import { Injectable, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { firstValueFrom } from 'rxjs';
import {
  ClientProject,
  CreateClientProjectRequest,
  ProjectResourcesSummary,
  UpdateClientProjectRequest,
} from '../models/project.model';

@Injectable({ providedIn: 'root' })
export class ProjectService {
  private readonly http = inject(HttpClient);

  private readonly _projects = signal<ClientProject[]>([]);
  private readonly _loading = signal(false);
  private readonly _error = signal<string | null>(null);

  readonly projects = this._projects.asReadonly();
  readonly loading = this._loading.asReadonly();
  readonly error = this._error.asReadonly();

  async fetchProjects(userId: number): Promise<void> {
    this._loading.set(true);
    this._error.set(null);
    try {
      const projects = await firstValueFrom(this.http.get<ClientProject[]>(`/api/infrastructure/projects/user/${userId}`));
      this._projects.set(Array.isArray(projects) ? projects : []);
    } catch (err: any) {
      this._error.set(err?.error?.message || 'Erreur lors du chargement des projets');
    } finally {
      this._loading.set(false);
    }
  }

  /** Another user's projects (admin pickers) — returned directly, leaves the shared `projects` signal alone. */
  async listForUser(userId: number): Promise<ClientProject[]> {
    try {
      const projects = await firstValueFrom(this.http.get<ClientProject[]>(`/api/infrastructure/projects/user/${userId}`));
      return Array.isArray(projects) ? projects : [];
    } catch {
      return [];
    }
  }

  async createProject(data: CreateClientProjectRequest): Promise<ClientProject> {
    this._loading.set(true);
    this._error.set(null);
    try {
      const project = await firstValueFrom(this.http.post<ClientProject>('/api/infrastructure/projects', data));
      this._projects.update((projects) => [...projects, project]);
      return project;
    } catch (err: any) {
      this._error.set(err?.error?.message || 'Erreur lors de la création du projet');
      throw err;
    } finally {
      this._loading.set(false);
    }
  }

  async renameProject(id: number, data: UpdateClientProjectRequest): Promise<ClientProject> {
    try {
      const project = await firstValueFrom(this.http.put<ClientProject>(`/api/infrastructure/projects/${id}`, data));
      this._projects.update((projects) => projects.map((p) => (p.id === id ? project : p)));
      return project;
    } catch (err: any) {
      this._error.set(err?.error?.message || 'Erreur lors du renommage du projet');
      throw err;
    }
  }

  async deleteProject(id: number): Promise<void> {
    try {
      await firstValueFrom(this.http.delete(`/api/infrastructure/projects/${id}`));
      this._projects.update((projects) => projects.filter((p) => p.id !== id));
    } catch (err: any) {
      this._error.set(err?.error?.message || 'Erreur lors de la suppression du projet');
      throw err;
    }
  }

  async getResources(id: number): Promise<ProjectResourcesSummary> {
    return firstValueFrom(this.http.get<ProjectResourcesSummary>(`/api/infrastructure/projects/${id}/resources`));
  }

  clearError(): void {
    this._error.set(null);
  }
}
