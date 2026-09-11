import { Injectable, computed, inject, signal } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { firstValueFrom } from 'rxjs';
import {
  BackupScheduleRequest,
  CreateVpsRequest,
  FlavorOption,
  ImageOption,
  InterfaceDetails,
  NetworkOption,
  SecurityGroupOption,
  Vm,
  VmHealth,
  VmMetadata,
  VpsActivityLog,
  VpsBackupSchedule,
} from '../models/vm.model';
import { PageResponse } from '../models/admin.model';

@Injectable({ providedIn: 'root' })
export class VmService {
  private readonly http = inject(HttpClient);

  private readonly _vms = signal<Vm[]>([]);
  private readonly _currentVm = signal<Vm | null>(null);
  private readonly _loading = signal(false);
  private readonly _error = signal<string | null>(null);

  readonly vms = this._vms.asReadonly();
  readonly currentVm = this._currentVm.asReadonly();
  readonly loading = this._loading.asReadonly();
  readonly error = this._error.asReadonly();

  readonly running = computed(() => this._vms().filter((v) => v.status === 'RUNNING').length);
  readonly stopped = computed(() => this._vms().filter((v) => v.status === 'STOPPED').length);
  readonly errored = computed(() => this._vms().filter((v) => v.status === 'ERROR').length);
  readonly totalCpu = computed(() => this._vms().reduce((s, v) => s + (v.cpu ?? 0), 0));
  readonly totalRam = computed(() => this._vms().reduce((s, v) => s + (v.ram ?? 0), 0));
  readonly totalStorage = computed(() => this._vms().reduce((s, v) => s + (v.storage ?? 0), 0));

  async fetchVms(userId: number): Promise<void> {
    this._loading.set(true);
    this._error.set(null);
    try {
      const vms = await firstValueFrom(this.http.get<Vm[]>(`/api/infrastructure/vps/user/${userId}`));
      this._vms.set(Array.isArray(vms) ? vms : []);
    } catch (err: any) {
      this._error.set(err?.error?.message || 'Erreur lors du chargement des VMs');
    } finally {
      this._loading.set(false);
    }
  }

  async createVm(data: CreateVpsRequest): Promise<Vm> {
    this._loading.set(true);
    this._error.set(null);
    try {
      const vm = await firstValueFrom(this.http.post<Vm>('/api/infrastructure/vps', data));
      this._vms.update((vms) => [...vms, vm]);
      return vm;
    } catch (err: any) {
      this._error.set(err?.error?.message || 'Erreur lors de la création de la VM');
      throw err;
    } finally {
      this._loading.set(false);
    }
  }

  async deleteVm(id: number): Promise<void> {
    this._loading.set(true);
    try {
      await firstValueFrom(this.http.delete(`/api/infrastructure/vps/${id}`));
      this._vms.update((vms) => vms.filter((v) => v.id !== id));
    } catch (err: any) {
      this._error.set(err?.error?.message || 'Erreur lors de la suppression');
      throw err;
    } finally {
      this._loading.set(false);
    }
  }

  async assignProject(id: number, projectId: number | null): Promise<void> {
    try {
      const updated = await firstValueFrom(this.http.put<Vm>(`/api/infrastructure/vps/${id}/project`, { projectId }));
      this.replaceVm(updated);
    } catch (err: any) {
      this._error.set(err?.error?.message || "Erreur lors de l'assignation du projet");
      throw err;
    }
  }

  async restartVm(id: number): Promise<void> {
    try {
      const updated = await firstValueFrom(this.http.post<Vm>(`/api/infrastructure/vps/${id}/restart`, {}));
      this.replaceVm(updated);
    } catch (err: any) {
      this._error.set(err?.error?.message || 'Erreur lors du redémarrage');
      throw err;
    }
  }

  async stopVm(id: number): Promise<void> {
    try {
      const updated = await firstValueFrom(this.http.post<Vm>(`/api/infrastructure/vps/${id}/stop`, {}));
      this.replaceVm(updated);
    } catch (err: any) {
      this._error.set(err?.error?.message || "Erreur lors de l'arrêt");
      throw err;
    }
  }

  async startVm(id: number): Promise<void> {
    try {
      const updated = await firstValueFrom(this.http.post<Vm>(`/api/infrastructure/vps/${id}/start`, {}));
      this.replaceVm(updated);
    } catch (err: any) {
      this._error.set(err?.error?.message || 'Erreur lors du démarrage');
      throw err;
    }
  }

  async refreshVmStatus(id: number): Promise<void> {
    try {
      const updated = await firstValueFrom(this.http.post<Vm>(`/api/infrastructure/vps/${id}/refresh`, {}));
      this.replaceVm(updated);
    } catch (err: any) {
      this._error.set(err?.error?.message || "Erreur lors de l'actualisation du statut");
      throw err;
    }
  }

  async getVmHealth(id: number): Promise<VmHealth> {
    return firstValueFrom(this.http.get<VmHealth>(`/api/infrastructure/vps/${id}/health`));
  }

  async allocateFloatingIp(id: number): Promise<void> {
    try {
      const updated = await firstValueFrom(this.http.post<Vm>(`/api/infrastructure/vps/${id}/floating-ip`, {}));
      this.replaceVm(updated);
    } catch (err: any) {
      this._error.set(err?.error?.message || "Erreur lors de l'allocation de l'IP flottante");
      throw err;
    }
  }

  async releaseFloatingIp(id: number): Promise<void> {
    try {
      const updated = await firstValueFrom(this.http.delete<Vm>(`/api/infrastructure/vps/${id}/floating-ip`));
      this.replaceVm(updated);
    } catch (err: any) {
      this._error.set(err?.error?.message || "Erreur lors de la libération de l'IP flottante");
      throw err;
    }
  }

  async fetchNetworks(): Promise<NetworkOption[]> {
    return firstValueFrom(this.http.get<NetworkOption[]>('/api/infrastructure/catalog/networks'));
  }

  async fetchSecurityGroups(): Promise<SecurityGroupOption[]> {
    return firstValueFrom(this.http.get<SecurityGroupOption[]>('/api/infrastructure/catalog/security-groups'));
  }

  async getVm(id: number): Promise<void> {
    this._loading.set(true);
    this._error.set(null);
    try {
      const vm = await firstValueFrom(this.http.get<Vm>(`/api/infrastructure/vps/${id}`));
      this._currentVm.set(vm);
    } catch (err: any) {
      this._error.set(err?.error?.message || 'Erreur lors du chargement de la VM');
      throw err;
    } finally {
      this._loading.set(false);
    }
  }

  async renameVm(id: number, name: string): Promise<void> {
    try {
      const params = new HttpParams().set('name', name);
      const updated = await firstValueFrom(this.http.patch<Vm>(`/api/infrastructure/vps/${id}/rename`, {}, { params }));
      this.replaceVm(updated);
    } catch (err: any) {
      this._error.set(err?.error?.message || 'Erreur lors du renommage');
      throw err;
    }
  }

  async fetchFlavors(): Promise<FlavorOption[]> {
    return firstValueFrom(this.http.get<FlavorOption[]>('/api/infrastructure/catalog/flavors'));
  }

  async resizeVm(id: number, flavorId: string): Promise<void> {
    try {
      const params = new HttpParams().set('flavorId', flavorId);
      const updated = await firstValueFrom(this.http.post<Vm>(`/api/infrastructure/vps/${id}/resize`, {}, { params }));
      this.replaceVm(updated);
    } catch (err: any) {
      this._error.set(err?.error?.message || 'Erreur lors du redimensionnement');
      throw err;
    }
  }

  async confirmResize(id: number): Promise<void> {
    try {
      const updated = await firstValueFrom(this.http.post<Vm>(`/api/infrastructure/vps/${id}/resize/confirm`, {}));
      this.replaceVm(updated);
    } catch (err: any) {
      this._error.set(err?.error?.message || 'Erreur lors de la confirmation du redimensionnement');
      throw err;
    }
  }

  async revertResize(id: number): Promise<void> {
    try {
      const updated = await firstValueFrom(this.http.post<Vm>(`/api/infrastructure/vps/${id}/resize/revert`, {}));
      this.replaceVm(updated);
    } catch (err: any) {
      this._error.set(err?.error?.message || "Erreur lors de l'annulation du redimensionnement");
      throw err;
    }
  }

  async snapshotVm(id: number, name: string): Promise<string> {
    try {
      const params = new HttpParams().set('name', name);
      const result = await firstValueFrom(
        this.http.post<{ imageId: string }>(`/api/infrastructure/vps/${id}/snapshot`, {}, { params }),
      );
      return result.imageId;
    } catch (err: any) {
      this._error.set(err?.error?.message || 'Erreur lors de la création du snapshot');
      throw err;
    }
  }

  async getCurrentSecurityGroups(id: number): Promise<SecurityGroupOption[]> {
    return firstValueFrom(this.http.get<SecurityGroupOption[]>(`/api/infrastructure/vps/${id}/security-groups`));
  }

  async assignSecurityGroup(id: number, name: string): Promise<void> {
    try {
      const params = new HttpParams().set('name', name);
      await firstValueFrom(this.http.post<void>(`/api/infrastructure/vps/${id}/security-groups`, {}, { params }));
    } catch (err: any) {
      this._error.set(err?.error?.message || "Erreur lors de l'ajout du groupe de sécurité");
      throw err;
    }
  }

  async removeSecurityGroup(id: number, name: string): Promise<void> {
    try {
      await firstValueFrom(this.http.delete<void>(`/api/infrastructure/vps/${id}/security-groups/${name}`));
    } catch (err: any) {
      this._error.set(err?.error?.message || 'Erreur lors du retrait du groupe de sécurité');
      throw err;
    }
  }

  async getMetadata(id: number): Promise<VmMetadata> {
    return firstValueFrom(this.http.get<VmMetadata>(`/api/infrastructure/vps/${id}/metadata`));
  }

  async updateMetadata(id: number, metadata: VmMetadata): Promise<VmMetadata> {
    try {
      return await firstValueFrom(this.http.put<VmMetadata>(`/api/infrastructure/vps/${id}/metadata`, metadata));
    } catch (err: any) {
      this._error.set(err?.error?.message || 'Erreur lors de la mise à jour des métadonnées');
      throw err;
    }
  }

  async deleteMetadataKey(id: number, key: string): Promise<void> {
    try {
      await firstValueFrom(this.http.delete<void>(`/api/infrastructure/vps/${id}/metadata/${key}`));
    } catch (err: any) {
      this._error.set(err?.error?.message || 'Erreur lors de la suppression de la métadonnée');
      throw err;
    }
  }

  async getActivity(id: number, page = 0, size = 20): Promise<PageResponse<VpsActivityLog>> {
    const params = new HttpParams().set('page', page).set('size', size);
    return firstValueFrom(this.http.get<PageResponse<VpsActivityLog>>(`/api/infrastructure/vps/${id}/activity`, { params }));
  }

  async rescueVm(id: number): Promise<string> {
    try {
      const result = await firstValueFrom(this.http.post<{ adminPass: string }>(`/api/infrastructure/vps/${id}/rescue`, {}));
      await this.getVm(id);
      return result.adminPass;
    } catch (err: any) {
      this._error.set(err?.error?.message || 'Erreur lors du passage en mode rescue');
      throw err;
    }
  }

  async unrescueVm(id: number): Promise<void> {
    try {
      const updated = await firstValueFrom(this.http.post<Vm>(`/api/infrastructure/vps/${id}/unrescue`, {}));
      this.replaceVm(updated);
    } catch (err: any) {
      this._error.set(err?.error?.message || 'Erreur lors de la sortie du mode rescue');
      throw err;
    }
  }

  private async simpleAction(id: number, action: string, errorMessage: string): Promise<void> {
    try {
      const updated = await firstValueFrom(this.http.post<Vm>(`/api/infrastructure/vps/${id}/${action}`, {}));
      this.replaceVm(updated);
    } catch (err: any) {
      this._error.set(err?.error?.message || errorMessage);
      throw err;
    }
  }

  async pauseVm(id: number): Promise<void> {
    return this.simpleAction(id, 'pause', 'Erreur lors de la mise en pause');
  }

  async unpauseVm(id: number): Promise<void> {
    return this.simpleAction(id, 'unpause', 'Erreur lors de la reprise');
  }

  async suspendVm(id: number): Promise<void> {
    return this.simpleAction(id, 'suspend', 'Erreur lors de la suspension');
  }

  async resumeVm(id: number): Promise<void> {
    return this.simpleAction(id, 'resume', 'Erreur lors de la reprise');
  }

  async shelveVm(id: number): Promise<void> {
    return this.simpleAction(id, 'shelve', 'Erreur lors de la mise en réserve');
  }

  async unshelveVm(id: number): Promise<void> {
    return this.simpleAction(id, 'unshelve', 'Erreur lors de la sortie de réserve');
  }

  async lockVm(id: number): Promise<void> {
    return this.simpleAction(id, 'lock', 'Erreur lors du verrouillage');
  }

  async unlockVm(id: number): Promise<void> {
    return this.simpleAction(id, 'unlock', 'Erreur lors du déverrouillage');
  }

  async softRebootVm(id: number): Promise<void> {
    return this.simpleAction(id, 'soft-reboot', 'Erreur lors du redémarrage');
  }

  async rebuildVm(id: number, imageId: string): Promise<void> {
    try {
      await firstValueFrom(this.http.post(`/api/infrastructure/vps/${id}/rebuild`, {}, { params: { imageId } }));
    } catch (err: any) {
      this._error.set(err?.error?.message || 'Erreur lors de la reconstruction');
      throw err;
    }
  }

  async getConsoleUrl(id: number): Promise<string> {
    const result = await firstValueFrom(this.http.get<{ url: string }>(`/api/infrastructure/vps/${id}/console`));
    return result.url;
  }

  async getConsoleLog(id: number, lines = 100): Promise<string> {
    const result = await firstValueFrom(
      this.http.get<{ log: string }>(`/api/infrastructure/vps/${id}/console-log`, { params: { lines } }),
    );
    return result.log;
  }

  async listInterfaces(id: number): Promise<InterfaceDetails[]> {
    return firstValueFrom(this.http.get<InterfaceDetails[]>(`/api/infrastructure/vps/${id}/interfaces`));
  }

  async attachInterface(id: number, networkId: string): Promise<InterfaceDetails> {
    try {
      return await firstValueFrom(
        this.http.post<InterfaceDetails>(`/api/infrastructure/vps/${id}/interfaces`, {}, { params: { networkId } }),
      );
    } catch (err: any) {
      this._error.set(err?.error?.message || "Erreur lors de l'attachement de l'interface");
      throw err;
    }
  }

  async detachInterface(id: number, attachmentId: string): Promise<void> {
    try {
      await firstValueFrom(this.http.delete(`/api/infrastructure/vps/${id}/interfaces/${attachmentId}`));
    } catch (err: any) {
      this._error.set(err?.error?.message || "Erreur lors du détachement de l'interface");
      throw err;
    }
  }

  async getPortSecurityGroups(id: number, portId: string): Promise<string[]> {
    return firstValueFrom(this.http.get<string[]>(`/api/infrastructure/vps/${id}/interfaces/${portId}/security-groups`));
  }

  async updatePortSecurityGroups(id: number, portId: string, groupIds: string[]): Promise<void> {
    try {
      await firstValueFrom(this.http.put(`/api/infrastructure/vps/${id}/interfaces/${portId}/security-groups`, groupIds));
    } catch (err: any) {
      this._error.set(err?.error?.message || 'Erreur lors de la mise à jour des groupes de sécurité du port');
      throw err;
    }
  }

  async getBackupSchedule(id: number): Promise<VpsBackupSchedule | null> {
    try {
      return await firstValueFrom(this.http.get<VpsBackupSchedule>(`/api/infrastructure/vps/${id}/backup-schedule`));
    } catch {
      return null;
    }
  }

  async saveBackupSchedule(id: number, data: BackupScheduleRequest): Promise<VpsBackupSchedule> {
    return firstValueFrom(this.http.put<VpsBackupSchedule>(`/api/infrastructure/vps/${id}/backup-schedule`, data));
  }

  async deleteBackupSchedule(id: number): Promise<void> {
    await firstValueFrom(this.http.delete(`/api/infrastructure/vps/${id}/backup-schedule`));
  }

  async listBackups(id: number): Promise<ImageOption[]> {
    return firstValueFrom(this.http.get<ImageOption[]>(`/api/infrastructure/vps/${id}/backups`));
  }

  clearError(): void {
    this._error.set(null);
  }

  private replaceVm(updated: Vm): void {
    this._vms.update((vms) => vms.map((v) => (v.id === updated.id ? updated : v)));
    if (this._currentVm()?.id === updated.id) {
      this._currentVm.set(updated);
    }
  }
}