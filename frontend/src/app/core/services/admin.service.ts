import { Injectable, inject, signal } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { firstValueFrom } from 'rxjs';
import {
  AdminDomain,
  AdminUser,
  AlertEvent,
  AlertRule,
  ApiLogEntry,
  ApiLogFilters,
  CreateAlertRuleRequest,
  K8sCluster,
  PageResponse,
  SmtpConfig,
  TicketAgent,
  UpdateSmtpConfigRequest,
} from '../models/admin.model';
import {
  ClientFloatingIp,
  ClientKeypair,
  ClientRouter,
  ClientServerGroup,
  Vm,
  Volume,
  VpsNetwork,
  VpsSecurityGroup,
  VpsVolumeSnapshot,
} from '../models/vm.model';
import { AdminClientSummary, ClientProject } from '../models/project.model';
import { CreateTicketForClientRequest, Ticket, TicketCategory, TicketPriority, TicketStatus } from '../models/support.model';

@Injectable({ providedIn: 'root' })
export class AdminService {
  private readonly http = inject(HttpClient);

  private readonly _smtpConfig = signal<SmtpConfig | null>(null);
  private readonly _users = signal<AdminUser[]>([]);
  private readonly _error = signal<string | null>(null);

  private readonly _allVps = signal<Vm[]>([]);
  private readonly _allVolumes = signal<Volume[]>([]);
  private readonly _allClusters = signal<K8sCluster[]>([]);
  private readonly _allDomains = signal<AdminDomain[]>([]);
  private readonly _allNetworks = signal<VpsNetwork[]>([]);
  private readonly _allRouters = signal<ClientRouter[]>([]);
  private readonly _allFloatingIps = signal<ClientFloatingIp[]>([]);
  private readonly _allSecurityGroups = signal<VpsSecurityGroup[]>([]);
  private readonly _allKeypairs = signal<ClientKeypair[]>([]);
  private readonly _allServerGroups = signal<ClientServerGroup[]>([]);
  private readonly _allVolumeSnapshots = signal<VpsVolumeSnapshot[]>([]);
  private readonly _allProjects = signal<ClientProject[]>([]);
  private readonly _clientSummaries = signal<AdminClientSummary[]>([]);

  private readonly _logs = signal<PageResponse<ApiLogEntry> | null>(null);
  private readonly _logServices = signal<string[]>([]);
  private readonly _activeAlerts = signal<AlertEvent[]>([]);
  private readonly _alertRules = signal<AlertRule[]>([]);
  private readonly _knownServices = signal<string[]>([]);

  private readonly _allTickets = signal<Ticket[]>([]);
  private readonly _ticketAgents = signal<TicketAgent[]>([]);
  private readonly _openTicketCount = signal(0);

  readonly smtpConfig = this._smtpConfig.asReadonly();
  readonly users = this._users.asReadonly();
  readonly error = this._error.asReadonly();

  readonly allVps = this._allVps.asReadonly();
  readonly allVolumes = this._allVolumes.asReadonly();
  readonly allClusters = this._allClusters.asReadonly();
  readonly allDomains = this._allDomains.asReadonly();
  readonly allNetworks = this._allNetworks.asReadonly();
  readonly allRouters = this._allRouters.asReadonly();
  readonly allFloatingIps = this._allFloatingIps.asReadonly();
  readonly allSecurityGroups = this._allSecurityGroups.asReadonly();
  readonly allKeypairs = this._allKeypairs.asReadonly();
  readonly allServerGroups = this._allServerGroups.asReadonly();
  readonly allVolumeSnapshots = this._allVolumeSnapshots.asReadonly();
  readonly allProjects = this._allProjects.asReadonly();
  readonly clientSummaries = this._clientSummaries.asReadonly();

  readonly logs = this._logs.asReadonly();
  readonly logServices = this._logServices.asReadonly();
  readonly activeAlerts = this._activeAlerts.asReadonly();
  readonly alertRules = this._alertRules.asReadonly();
  readonly knownServices = this._knownServices.asReadonly();

  readonly allTickets = this._allTickets.asReadonly();
  readonly ticketAgents = this._ticketAgents.asReadonly();
  /** Tickets currently OPEN across all clients — i.e. needing agent attention (new or just replied-to by a client). */
  readonly openTicketCount = this._openTicketCount.asReadonly();

  async refreshOpenTicketCount(): Promise<void> {
    try {
      const params = new HttpParams().set('status', 'OPEN');
      const tickets = await firstValueFrom(this.http.get<Ticket[]>('/api/support/admin/tickets', { params }));
      this._openTicketCount.set(Array.isArray(tickets) ? tickets.length : 0);
    } catch {
      // Non-critical (also expectedly fails for a non-agent admin) — badge just stays at 0.
    }
  }

  async loadSmtpConfig(): Promise<void> {
    this._error.set(null);
    try {
      const config = await firstValueFrom(this.http.get<SmtpConfig>('/api/auth/admin/smtp-config'));
      this._smtpConfig.set(config);
    } catch (err: any) {
      this._error.set(err?.error?.message || 'Erreur lors du chargement de la configuration SMTP');
      throw err;
    }
  }

  async updateSmtpConfig(data: UpdateSmtpConfigRequest): Promise<void> {
    this._error.set(null);
    try {
      const config = await firstValueFrom(this.http.put<SmtpConfig>('/api/auth/admin/smtp-config', data));
      this._smtpConfig.set(config);
    } catch (err: any) {
      this._error.set(err?.error?.message || 'Erreur lors de la mise à jour de la configuration SMTP');
      throw err;
    }
  }

  async sendTestEmail(recipient: string): Promise<void> {
    this._error.set(null);
    try {
      await firstValueFrom(this.http.post<void>('/api/auth/admin/smtp-config/test', { recipient }));
    } catch (err: any) {
      this._error.set(err?.error?.message || "Erreur lors de l'envoi de l'email de test");
      throw err;
    }
  }

  async loadUsers(): Promise<void> {
    this._error.set(null);
    try {
      const users = await firstValueFrom(this.http.get<AdminUser[]>('/api/auth/admin/users'));
      this._users.set(users);
    } catch (err: any) {
      this._error.set(err?.error?.message || 'Erreur lors du chargement des utilisateurs');
      throw err;
    }
  }

  async updateUserRole(id: number, role: string): Promise<void> {
    this._error.set(null);
    try {
      const user = await firstValueFrom(this.http.put<AdminUser>(`/api/auth/admin/users/${id}/role`, { role }));
      this._users.update((users) => users.map((u) => (u.id === id ? user : u)));
    } catch (err: any) {
      this._error.set(err?.error?.message || 'Erreur lors de la mise à jour du rôle');
      throw err;
    }
  }

  async setUserEnabled(id: number, enabled: boolean): Promise<void> {
    this._error.set(null);
    try {
      const user = await firstValueFrom(this.http.put<AdminUser>(`/api/auth/admin/users/${id}/enabled`, { enabled }));
      this._users.update((users) => users.map((u) => (u.id === id ? user : u)));
    } catch (err: any) {
      this._error.set(err?.error?.message || 'Erreur lors de la mise à jour du compte');
      throw err;
    }
  }

  async deleteUser(id: number): Promise<void> {
    this._error.set(null);
    try {
      await firstValueFrom(this.http.delete<void>(`/api/auth/admin/users/${id}`));
      this._users.update((users) => users.filter((u) => u.id !== id));
    } catch (err: any) {
      this._error.set(err?.error?.message || "Erreur lors de la suppression de l'utilisateur");
      throw err;
    }
  }

  async loadAllVps(): Promise<void> {
    this._error.set(null);
    try {
      const vps = await firstValueFrom(this.http.get<Vm[]>('/api/infrastructure/admin/vps'));
      this._allVps.set(Array.isArray(vps) ? vps : []);
    } catch (err: any) {
      this._error.set(err?.error?.message || 'Erreur lors du chargement des VMs');
      throw err;
    }
  }

  async loadAllVolumes(): Promise<void> {
    this._error.set(null);
    try {
      const volumes = await firstValueFrom(this.http.get<Volume[]>('/api/infrastructure/admin/volumes'));
      this._allVolumes.set(Array.isArray(volumes) ? volumes : []);
    } catch (err: any) {
      this._error.set(err?.error?.message || 'Erreur lors du chargement des volumes');
      throw err;
    }
  }

  async loadAllClusters(): Promise<void> {
    this._error.set(null);
    try {
      const clusters = await firstValueFrom(this.http.get<K8sCluster[]>('/api/infrastructure/admin/clusters'));
      this._allClusters.set(Array.isArray(clusters) ? clusters : []);
    } catch (err: any) {
      this._error.set(err?.error?.message || 'Erreur lors du chargement des clusters');
      throw err;
    }
  }

  async loadAllDomains(): Promise<void> {
    this._error.set(null);
    try {
      const domains = await firstValueFrom(this.http.get<AdminDomain[]>('/api/infrastructure/admin/domains'));
      this._allDomains.set(Array.isArray(domains) ? domains : []);
    } catch (err: any) {
      this._error.set(err?.error?.message || 'Erreur lors du chargement des domaines');
      throw err;
    }
  }

  async loadAllRouters(): Promise<void> {
    this._error.set(null);
    try {
      const routers = await firstValueFrom(this.http.get<ClientRouter[]>('/api/infrastructure/admin/routers'));
      this._allRouters.set(Array.isArray(routers) ? routers : []);
    } catch (err: any) {
      this._error.set(err?.error?.message || 'Erreur lors du chargement des routeurs');
      throw err;
    }
  }

  async loadAllFloatingIps(): Promise<void> {
    this._error.set(null);
    try {
      const ips = await firstValueFrom(this.http.get<ClientFloatingIp[]>('/api/infrastructure/admin/floating-ips'));
      this._allFloatingIps.set(Array.isArray(ips) ? ips : []);
    } catch (err: any) {
      this._error.set(err?.error?.message || 'Erreur lors du chargement des IP flottantes');
      throw err;
    }
  }

  async loadAllNetworks(): Promise<void> {
    this._error.set(null);
    try {
      const networks = await firstValueFrom(this.http.get<VpsNetwork[]>('/api/infrastructure/admin/networks'));
      this._allNetworks.set(Array.isArray(networks) ? networks : []);
    } catch (err: any) {
      this._error.set(err?.error?.message || 'Erreur lors du chargement des réseaux');
      throw err;
    }
  }

  async loadAllSecurityGroups(): Promise<void> {
    this._error.set(null);
    try {
      const groups = await firstValueFrom(
        this.http.get<VpsSecurityGroup[]>('/api/infrastructure/admin/security-groups'),
      );
      this._allSecurityGroups.set(Array.isArray(groups) ? groups : []);
    } catch (err: any) {
      this._error.set(err?.error?.message || 'Erreur lors du chargement des groupes de sécurité');
      throw err;
    }
  }

  async loadAllKeypairs(): Promise<void> {
    this._error.set(null);
    try {
      const keypairs = await firstValueFrom(this.http.get<ClientKeypair[]>('/api/infrastructure/admin/keypairs'));
      this._allKeypairs.set(Array.isArray(keypairs) ? keypairs : []);
    } catch (err: any) {
      this._error.set(err?.error?.message || 'Erreur lors du chargement des paires de clés');
      throw err;
    }
  }

  async loadAllServerGroups(): Promise<void> {
    this._error.set(null);
    try {
      const groups = await firstValueFrom(this.http.get<ClientServerGroup[]>('/api/infrastructure/admin/server-groups'));
      this._allServerGroups.set(Array.isArray(groups) ? groups : []);
    } catch (err: any) {
      this._error.set(err?.error?.message || 'Erreur lors du chargement des groupes de serveurs');
      throw err;
    }
  }

  async loadAllVolumeSnapshots(): Promise<void> {
    this._error.set(null);
    try {
      const snapshots = await firstValueFrom(
        this.http.get<VpsVolumeSnapshot[]>('/api/infrastructure/admin/volume-snapshots'),
      );
      this._allVolumeSnapshots.set(Array.isArray(snapshots) ? snapshots : []);
    } catch (err: any) {
      this._error.set(err?.error?.message || 'Erreur lors du chargement des snapshots');
      throw err;
    }
  }

  async loadAllProjects(): Promise<void> {
    this._error.set(null);
    try {
      const projects = await firstValueFrom(this.http.get<ClientProject[]>('/api/infrastructure/admin/projects'));
      this._allProjects.set(Array.isArray(projects) ? projects : []);
    } catch (err: any) {
      this._error.set(err?.error?.message || 'Erreur lors du chargement des projets');
      throw err;
    }
  }

  async loadClientSummaries(): Promise<void> {
    this._error.set(null);
    try {
      const summaries = await firstValueFrom(
        this.http.get<AdminClientSummary[]>('/api/infrastructure/admin/clients'),
      );
      this._clientSummaries.set(Array.isArray(summaries) ? summaries : []);
    } catch (err: any) {
      this._error.set(err?.error?.message || 'Erreur lors du chargement des clients');
      throw err;
    }
  }

  async loadApiLogs(filters: ApiLogFilters = {}): Promise<void> {
    this._error.set(null);
    try {
      let params = new HttpParams()
        .set('page', String(filters.page ?? 0))
        .set('size', String(filters.size ?? 50));
      if (filters.service) params = params.set('service', filters.service);
      if (filters.errorsOnly) params = params.set('errorsOnly', 'true');
      if (filters.search) params = params.set('search', filters.search);

      const result = await firstValueFrom(
        this.http.get<PageResponse<ApiLogEntry>>('/api/monitoring/logs', { params }),
      );
      this._logs.set(result);
    } catch (err: any) {
      this._error.set(err?.error?.message || 'Erreur lors du chargement des logs API');
      throw err;
    }
  }

  async loadLogServices(): Promise<void> {
    try {
      const services = await firstValueFrom(this.http.get<string[]>('/api/monitoring/logs/services'));
      this._logServices.set(Array.isArray(services) ? services : []);
    } catch {
      // Non-critical — filter dropdown just stays empty.
    }
  }

  async loadActiveAlerts(): Promise<void> {
    this._error.set(null);
    try {
      const alerts = await firstValueFrom(this.http.get<AlertEvent[]>('/api/monitoring/alerts/active'));
      this._activeAlerts.set(Array.isArray(alerts) ? alerts : []);
    } catch (err: any) {
      this._error.set(err?.error?.message || 'Erreur lors du chargement des alertes');
      throw err;
    }
  }

  async loadAlertRules(): Promise<void> {
    this._error.set(null);
    try {
      const rules = await firstValueFrom(this.http.get<AlertRule[]>('/api/monitoring/alerts/rules'));
      this._alertRules.set(Array.isArray(rules) ? rules : []);
    } catch (err: any) {
      this._error.set(err?.error?.message || 'Erreur lors du chargement des règles');
      throw err;
    }
  }

  async createAlertRule(data: CreateAlertRuleRequest): Promise<void> {
    this._error.set(null);
    try {
      const rule = await firstValueFrom(this.http.post<AlertRule>('/api/monitoring/alerts/rules', data));
      this._alertRules.update((rules) => [rule, ...rules]);
    } catch (err: any) {
      this._error.set(err?.error?.message || 'Erreur lors de la création de la règle');
      throw err;
    }
  }

  async setAlertRuleEnabled(id: number, enabled: boolean): Promise<void> {
    this._error.set(null);
    try {
      const rule = await firstValueFrom(
        this.http.patch<AlertRule>(`/api/monitoring/alerts/rules/${id}/enabled`, { enabled }),
      );
      this._alertRules.update((rules) => rules.map((r) => (r.id === id ? rule : r)));
    } catch (err: any) {
      this._error.set(err?.error?.message || 'Erreur lors de la mise à jour de la règle');
      throw err;
    }
  }

  async deleteAlertRule(id: number): Promise<void> {
    this._error.set(null);
    try {
      await firstValueFrom(this.http.delete<void>(`/api/monitoring/alerts/rules/${id}`));
      this._alertRules.update((rules) => rules.filter((r) => r.id !== id));
    } catch (err: any) {
      this._error.set(err?.error?.message || 'Erreur lors de la suppression de la règle');
      throw err;
    }
  }

  async loadKnownServices(): Promise<void> {
    try {
      const services = await firstValueFrom(this.http.get<string[]>('/api/monitoring/services'));
      this._knownServices.set(Array.isArray(services) ? services : []);
    } catch {
      // Non-critical — service dropdown just falls back to "Tous les services".
    }
  }

  async loadAllTickets(filters: { status?: TicketStatus; priority?: TicketPriority; category?: TicketCategory } = {}): Promise<void> {
    this._error.set(null);
    try {
      let params = new HttpParams();
      if (filters.status) params = params.set('status', filters.status);
      if (filters.priority) params = params.set('priority', filters.priority);
      if (filters.category) params = params.set('category', filters.category);

      const tickets = await firstValueFrom(this.http.get<Ticket[]>('/api/support/admin/tickets', { params }));
      this._allTickets.set(Array.isArray(tickets) ? tickets : []);
    } catch (err: any) {
      this._error.set(err?.error?.message || 'Erreur lors du chargement des tickets');
      throw err;
    }
  }

  async createTicketForClient(data: CreateTicketForClientRequest): Promise<Ticket> {
    this._error.set(null);
    try {
      return await firstValueFrom(this.http.post<Ticket>('/api/support/admin/tickets', data));
    } catch (err: any) {
      this._error.set(err?.error?.message || 'Erreur lors de la création du ticket');
      throw err;
    }
  }

  async assignTicket(id: number, agentId: number): Promise<Ticket> {
    this._error.set(null);
    try {
      const params = new HttpParams().set('agentId', String(agentId));
      const ticket = await firstValueFrom(this.http.patch<Ticket>(`/api/support/tickets/${id}/assign`, {}, { params }));
      this._allTickets.update((tickets) => tickets.map((t) => (t.id === id ? ticket : t)));
      return ticket;
    } catch (err: any) {
      this._error.set(err?.error?.message || "Erreur lors de l'assignation du ticket");
      throw err;
    }
  }

  async loadTicketAgents(): Promise<void> {
    this._error.set(null);
    try {
      const agents = await firstValueFrom(this.http.get<TicketAgent[]>('/api/support/agents'));
      this._ticketAgents.set(Array.isArray(agents) ? agents : []);
    } catch (err: any) {
      this._error.set(err?.error?.message || 'Erreur lors du chargement des agents');
      throw err;
    }
  }

  async grantTicketAgent(userId: number): Promise<void> {
    this._error.set(null);
    try {
      const agent = await firstValueFrom(this.http.post<TicketAgent>(`/api/support/agents/${userId}`, {}));
      this._ticketAgents.update((agents) => (agents.some((a) => a.userId === userId) ? agents : [agent, ...agents]));
    } catch (err: any) {
      this._error.set(err?.error?.message || "Erreur lors de l'octroi de la permission");
      throw err;
    }
  }

  async revokeTicketAgent(userId: number): Promise<void> {
    this._error.set(null);
    try {
      await firstValueFrom(this.http.delete<void>(`/api/support/agents/${userId}`));
      this._ticketAgents.update((agents) => agents.filter((a) => a.userId !== userId));
    } catch (err: any) {
      this._error.set(err?.error?.message || 'Erreur lors du retrait de la permission');
      throw err;
    }
  }

  clearError(): void {
    this._error.set(null);
  }
}