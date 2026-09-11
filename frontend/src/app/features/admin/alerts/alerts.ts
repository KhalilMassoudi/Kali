import { Component, computed, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ButtonModule } from 'primeng/button';
import { TagModule } from 'primeng/tag';
import { SelectModule } from 'primeng/select';
import { InputNumberModule } from 'primeng/inputnumber';
import { ToggleSwitchModule } from 'primeng/toggleswitch';
import { TooltipModule } from 'primeng/tooltip';
import { AdminService } from '../../../core/services/admin.service';
import { AlertSeverity, AlertType } from '../../../core/models/admin.model';

const TYPE_LABELS: Record<AlertType, string> = {
  CPU_HIGH: 'CPU élevé',
  MEMORY_HIGH: 'Mémoire élevée',
  RESPONSE_TIME_HIGH: 'Temps de réponse élevé',
  SERVICE_DOWN: 'Service injoignable',
  ERROR_RATE_HIGH: "Taux d'erreur élevé",
};

const SEVERITY_SEVERITIES: Record<AlertSeverity, 'info' | 'warn' | 'danger'> = {
  INFO: 'info',
  WARNING: 'warn',
  CRITICAL: 'danger',
};

const TYPE_OPTIONS: { label: string; value: AlertType }[] = (
  Object.keys(TYPE_LABELS) as AlertType[]
).map((value) => ({ label: TYPE_LABELS[value], value }));

interface Suggestion {
  label: string;
  serviceName: string | null;
  alertType: AlertType;
  threshold: number;
  hint: string;
}

const SUGGESTIONS: Suggestion[] = [
  {
    label: 'Service injoignable',
    serviceName: null,
    alertType: 'SERVICE_DOWN',
    threshold: 0,
    hint: 'Se déclenche dès qu’un service ne répond plus au health check — le plus important à activer en premier.',
  },
  {
    label: 'CPU > 80%',
    serviceName: null,
    alertType: 'CPU_HIGH',
    threshold: 80,
    hint: 'Seuil raisonnable avant saturation sur des VMs modestes.',
  },
  {
    label: 'Mémoire > 85%',
    serviceName: null,
    alertType: 'MEMORY_HIGH',
    threshold: 85,
    hint: 'La JVM tolère bien 80-90% avant le vrai risque d’OutOfMemory.',
  },
  {
    label: 'Temps de réponse > 2000 ms',
    serviceName: null,
    alertType: 'RESPONSE_TIME_HIGH',
    threshold: 2000,
    hint: 'Repère un service qui rame avant que les utilisateurs ne s’en plaignent.',
  },
  {
    label: "Taux d'erreur > 10%",
    serviceName: null,
    alertType: 'ERROR_RATE_HIGH',
    threshold: 10,
    hint: 'Utile seulement une fois du trafic réel généré (calculé sur http.server.requests).',
  },
];

@Component({
  selector: 'app-admin-alerts',
  imports: [CommonModule, FormsModule, ButtonModule, TagModule, SelectModule, InputNumberModule, ToggleSwitchModule, TooltipModule],
  templateUrl: './alerts.html',
  styleUrl: './alerts.scss',
})
export class AdminAlerts {
  readonly admin = inject(AdminService);

  readonly loading = signal(true);
  readonly error = this.admin.error;
  readonly alerts = this.admin.activeAlerts;
  readonly rules = this.admin.alertRules;

  readonly critical = computed(() => this.alerts().filter((a) => a.severity === 'CRITICAL').length);
  readonly warning = computed(() => this.alerts().filter((a) => a.severity === 'WARNING').length);

  readonly typeLabel = (type: AlertType) => TYPE_LABELS[type] ?? type;
  readonly severityTag = (severity: AlertSeverity) => SEVERITY_SEVERITIES[severity] ?? 'info';

  readonly suggestions = SUGGESTIONS;
  readonly typeOptions = TYPE_OPTIONS;

  get serviceOptions(): { label: string; value: string | null }[] {
    return [{ label: 'Tous les services', value: null }, ...this.admin.knownServices().map((s) => ({ label: s, value: s }))];
  }

  readonly formService = signal<string | null>(null);
  readonly formType = signal<AlertType>('SERVICE_DOWN');
  readonly formThreshold = signal<number>(0);
  readonly creating = signal(false);
  readonly formError = signal('');
  readonly ruleActionLoading = signal<Record<number, boolean>>({});

  constructor() {
    this.load();
  }

  async refresh(): Promise<void> {
    await this.load();
  }

  applySuggestion(s: Suggestion): void {
    this.formService.set(s.serviceName);
    this.formType.set(s.alertType);
    this.formThreshold.set(s.threshold);
  }

  async createRule(): Promise<void> {
    this.formError.set('');
    this.creating.set(true);
    try {
      await this.admin.createAlertRule({
        serviceName: this.formService(),
        alertType: this.formType(),
        threshold: this.formThreshold(),
      });
    } catch {
      this.formError.set(this.admin.error() || 'Erreur lors de la création de la règle');
    } finally {
      this.creating.set(false);
    }
  }

  async toggleRule(id: number, enabled: boolean): Promise<void> {
    this.ruleActionLoading.update((m) => ({ ...m, [id]: true }));
    try {
      await this.admin.setAlertRuleEnabled(id, enabled);
    } finally {
      this.ruleActionLoading.update((m) => ({ ...m, [id]: false }));
    }
  }

  async deleteRule(id: number): Promise<void> {
    this.ruleActionLoading.update((m) => ({ ...m, [id]: true }));
    try {
      await this.admin.deleteAlertRule(id);
    } finally {
      this.ruleActionLoading.update((m) => ({ ...m, [id]: false }));
    }
  }

  private async load(): Promise<void> {
    this.loading.set(true);
    try {
      await Promise.all([this.admin.loadActiveAlerts(), this.admin.loadAlertRules(), this.admin.loadKnownServices()]);
    } finally {
      this.loading.set(false);
    }
  }
}
