import { Injectable, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { firstValueFrom } from 'rxjs';
import { AddSecurityGroupRuleRequest, CreateSecurityGroupRequest, SecurityGroupRuleOption, VpsSecurityGroup } from '../models/vm.model';

@Injectable({ providedIn: 'root' })
export class SecurityGroupService {
  private readonly http = inject(HttpClient);

  private readonly _groups = signal<VpsSecurityGroup[]>([]);
  private readonly _rules = signal<Record<number, SecurityGroupRuleOption[]>>({});
  private readonly _loading = signal(false);
  private readonly _error = signal<string | null>(null);

  readonly groups = this._groups.asReadonly();
  readonly loading = this._loading.asReadonly();
  readonly error = this._error.asReadonly();

  rulesFor(groupId: number): SecurityGroupRuleOption[] {
    return this._rules()[groupId] ?? [];
  }

  async fetchGroups(userId: number): Promise<void> {
    this._loading.set(true);
    this._error.set(null);
    try {
      const groups = await firstValueFrom(
        this.http.get<VpsSecurityGroup[]>(`/api/infrastructure/security-groups/user/${userId}`),
      );
      this._groups.set(Array.isArray(groups) ? groups : []);
    } catch (err: any) {
      this._error.set(err?.error?.message || 'Erreur lors du chargement des groupes de sécurité');
    } finally {
      this._loading.set(false);
    }
  }

  async createGroup(data: CreateSecurityGroupRequest): Promise<VpsSecurityGroup> {
    this._loading.set(true);
    this._error.set(null);
    try {
      const group = await firstValueFrom(this.http.post<VpsSecurityGroup>('/api/infrastructure/security-groups', data));
      this._groups.update((groups) => [...groups, group]);
      return group;
    } catch (err: any) {
      this._error.set(err?.error?.message || 'Erreur lors de la création du groupe de sécurité');
      throw err;
    } finally {
      this._loading.set(false);
    }
  }

  async assignProject(id: number, projectId: number | null): Promise<void> {
    try {
      const updated = await firstValueFrom(
        this.http.put<VpsSecurityGroup>(`/api/infrastructure/security-groups/${id}/project`, { projectId }),
      );
      this._groups.update((groups) => groups.map((g) => (g.id === id ? updated : g)));
    } catch (err: any) {
      this._error.set(err?.error?.message || "Erreur lors de l'assignation du projet");
      throw err;
    }
  }

  async deleteGroup(id: number): Promise<void> {
    try {
      await firstValueFrom(this.http.delete(`/api/infrastructure/security-groups/${id}`));
      this._groups.update((groups) => groups.filter((g) => g.id !== id));
    } catch (err: any) {
      this._error.set(err?.error?.message || 'Erreur lors de la suppression du groupe de sécurité');
      throw err;
    }
  }

  async fetchRules(groupId: number): Promise<void> {
    try {
      const rules = await firstValueFrom(
        this.http.get<SecurityGroupRuleOption[]>(`/api/infrastructure/security-groups/${groupId}/rules`),
      );
      this._rules.update((all) => ({ ...all, [groupId]: rules ?? [] }));
    } catch (err: any) {
      this._error.set(err?.error?.message || 'Erreur lors du chargement des règles');
      throw err;
    }
  }

  async addRule(groupId: number, data: AddSecurityGroupRuleRequest): Promise<void> {
    try {
      await firstValueFrom(this.http.post(`/api/infrastructure/security-groups/${groupId}/rules`, data));
      await this.fetchRules(groupId);
    } catch (err: any) {
      this._error.set(err?.error?.message || "Erreur lors de l'ajout de la règle");
      throw err;
    }
  }

  async removeRule(groupId: number, ruleId: string): Promise<void> {
    try {
      await firstValueFrom(this.http.delete(`/api/infrastructure/security-groups/${groupId}/rules/${ruleId}`));
      await this.fetchRules(groupId);
    } catch (err: any) {
      this._error.set(err?.error?.message || 'Erreur lors de la suppression de la règle');
      throw err;
    }
  }

  clearError(): void {
    this._error.set(null);
  }
}
