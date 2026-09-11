import { Component, EventEmitter, Output, inject, model, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { DialogModule } from 'primeng/dialog';
import { InputTextModule } from 'primeng/inputtext';
import { SelectModule } from 'primeng/select';
import { ButtonModule } from 'primeng/button';
import { MessageModule } from 'primeng/message';
import { AuthService } from '../../../core/services/auth.service';
import { ServerGroupService } from '../../../core/services/server-group.service';
import { ServerGroupPolicy } from '../../../core/models/vm.model';

const POLICIES: { label: string; value: ServerGroupPolicy }[] = [
  { label: 'Affinité — mêmes hôtes', value: 'AFFINITY' },
  { label: 'Anti-affinité — hôtes différents', value: 'ANTI_AFFINITY' },
  { label: 'Affinité souple', value: 'SOFT_AFFINITY' },
  { label: 'Anti-affinité souple', value: 'SOFT_ANTI_AFFINITY' },
];

@Component({
  selector: 'app-create-server-group-dialog',
  imports: [CommonModule, ReactiveFormsModule, DialogModule, InputTextModule, SelectModule, ButtonModule, MessageModule],
  templateUrl: './create-server-group-dialog.html',
  styleUrl: './create-server-group-dialog.scss',
})
export class CreateServerGroupDialog {
  private readonly fb = inject(FormBuilder);
  private readonly auth = inject(AuthService);
  private readonly groupService = inject(ServerGroupService);

  readonly visible = model.required<boolean>();
  @Output() readonly created = new EventEmitter<void>();

  readonly loading = this.groupService.loading;
  readonly error = signal('');
  readonly policies = POLICIES;

  readonly form = this.fb.nonNullable.group({
    name: ['', [Validators.required, Validators.minLength(2)]],
    policy: ['ANTI_AFFINITY' as ServerGroupPolicy, Validators.required],
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
    const { name, policy } = this.form.getRawValue();
    try {
      await this.groupService.createGroup({ userId: user?.id ?? 1, name: name.trim(), policy });
      this.created.emit();
      this.close();
      this.form.reset({ name: '', policy: 'ANTI_AFFINITY' });
    } catch {
      this.error.set(this.groupService.error() || 'Erreur lors de la création du groupe');
    }
  }
}
