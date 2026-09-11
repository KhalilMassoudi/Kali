import { Component, EventEmitter, Output, computed, inject, input, model, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { DialogModule } from 'primeng/dialog';
import { InputTextModule } from 'primeng/inputtext';
import { SelectModule } from 'primeng/select';
import { ButtonModule } from 'primeng/button';
import { MessageModule } from 'primeng/message';
import { AuthService } from '../../../core/services/auth.service';
import { RouterService } from '../../../core/services/router.service';
import { NetworkOption } from '../../../core/models/vm.model';

@Component({
  selector: 'app-create-router-dialog',
  imports: [CommonModule, ReactiveFormsModule, DialogModule, InputTextModule, SelectModule, ButtonModule, MessageModule],
  templateUrl: './create-router-dialog.html',
  styleUrl: './create-router-dialog.scss',
})
export class CreateRouterDialog {
  private readonly fb = inject(FormBuilder);
  private readonly auth = inject(AuthService);
  private readonly routerService = inject(RouterService);

  readonly visible = model.required<boolean>();
  readonly externalNetworks = input<NetworkOption[]>([]);
  readonly targetUserId = input<number | null>(null);
  @Output() readonly created = new EventEmitter<void>();

  readonly loading = this.routerService.loading;
  readonly error = signal('');

  readonly gatewayOptions = computed(() => [
    { label: 'Aucune passerelle', value: null },
    ...this.externalNetworks().map((n) => ({ label: n.name, value: n.id })),
  ]);

  readonly form = this.fb.nonNullable.group({
    name: ['', [Validators.required, Validators.minLength(3), Validators.pattern(/^[a-z0-9-]+$/)]],
    externalNetworkId: this.fb.control<string | null>(null),
  });

  close(): void {
    this.visible.set(false);
  }

  async submit(): Promise<void> {
    this.error.set('');
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const user = this.auth.user();
    const { name, externalNetworkId } = this.form.getRawValue();
    try {
      await this.routerService.createRouter({
        userId: this.targetUserId() ?? user?.id ?? 1,
        name: name.trim(),
        externalNetworkId: externalNetworkId ?? undefined,
      });
      this.created.emit();
      this.close();
      this.form.reset({ name: '', externalNetworkId: null });
    } catch {
      this.error.set(this.routerService.error() || 'Erreur lors de la création du routeur');
    }
  }
}
