import { Component, EventEmitter, Output, computed, effect, inject, input, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { DialogModule } from 'primeng/dialog';
import { InputTextModule } from 'primeng/inputtext';
import { InputNumberModule } from 'primeng/inputnumber';
import { SelectModule } from 'primeng/select';
import { ButtonModule } from 'primeng/button';
import { MessageModule } from 'primeng/message';
import { SecurityGroupService } from '../../../core/services/security-group.service';
import { VpsSecurityGroup } from '../../../core/models/vm.model';

const DIRECTION_OPTIONS = [
  { value: 'ingress', label: 'Entrant (ingress)' },
  { value: 'egress', label: 'Sortant (egress)' },
];

const PROTOCOL_OPTIONS = [
  { value: 'tcp', label: 'TCP' },
  { value: 'udp', label: 'UDP' },
  { value: 'icmp', label: 'ICMP' },
];

@Component({
  selector: 'app-security-group-rules-dialog',
  imports: [CommonModule, FormsModule, DialogModule, InputTextModule, InputNumberModule, SelectModule, ButtonModule, MessageModule],
  templateUrl: './security-group-rules-dialog.html',
  styleUrl: './security-group-rules-dialog.scss',
})
export class SecurityGroupRulesDialog {
  readonly groupService = inject(SecurityGroupService);

  readonly group = input<VpsSecurityGroup | null>(null);
  @Output() readonly closed = new EventEmitter<void>();

  readonly visible = computed(() => this.group() !== null);
  readonly rules = computed(() => {
    const g = this.group();
    return g ? this.groupService.rulesFor(g.id) : [];
  });

  readonly directionOptions = DIRECTION_OPTIONS;
  readonly protocolOptions = PROTOCOL_OPTIONS;

  readonly direction = signal<'ingress' | 'egress'>('ingress');
  readonly protocol = signal<'tcp' | 'udp' | 'icmp'>('tcp');
  readonly portMin = signal<number | null>(80);
  readonly portMax = signal<number | null>(80);
  readonly cidr = signal('0.0.0.0/0');
  readonly loading = signal(false);
  readonly error = signal('');

  constructor() {
    effect(() => {
      const g = this.group();
      if (g) {
        this.error.set('');
        this.loading.set(true);
        this.groupService.fetchRules(g.id).finally(() => this.loading.set(false));
      }
    });
  }

  onHide(): void {
    this.closed.emit();
  }

  async addRule(): Promise<void> {
    const g = this.group();
    if (!g) return;
    this.error.set('');
    try {
      await this.groupService.addRule(g.id, {
        direction: this.direction(),
        protocol: this.protocol(),
        portMin: this.portMin() ?? undefined,
        portMax: this.portMax() ?? undefined,
        cidr: this.cidr() || '0.0.0.0/0',
      });
    } catch {
      this.error.set(this.groupService.error() || "Erreur lors de l'ajout de la règle");
    }
  }

  async removeRule(ruleId: string): Promise<void> {
    const g = this.group();
    if (!g) return;
    try {
      await this.groupService.removeRule(g.id, ruleId);
    } catch {
      this.error.set(this.groupService.error() || 'Erreur lors de la suppression de la règle');
    }
  }
}
