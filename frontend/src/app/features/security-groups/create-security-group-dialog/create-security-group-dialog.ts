import { Component, EventEmitter, Output, inject, input, model, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { DialogModule } from 'primeng/dialog';
import { InputTextModule } from 'primeng/inputtext';
import { ButtonModule } from 'primeng/button';
import { MessageModule } from 'primeng/message';
import { AuthService } from '../../../core/services/auth.service';
import { SecurityGroupService } from '../../../core/services/security-group.service';

@Component({
  selector: 'app-create-security-group-dialog',
  imports: [CommonModule, ReactiveFormsModule, DialogModule, InputTextModule, ButtonModule, MessageModule],
  templateUrl: './create-security-group-dialog.html',
  styleUrl: './create-security-group-dialog.scss',
})
export class CreateSecurityGroupDialog {
  private readonly fb = inject(FormBuilder);
  private readonly auth = inject(AuthService);
  private readonly groupService = inject(SecurityGroupService);

  readonly visible = model.required<boolean>();
  readonly targetUserId = input<number | null>(null);
  @Output() readonly created = new EventEmitter<void>();

  readonly loading = this.groupService.loading;
  readonly error = signal('');

  readonly form = this.fb.nonNullable.group({
    name: ['', [Validators.required, Validators.minLength(3), Validators.pattern(/^[a-z0-9-]+$/)]],
    description: [''],
  });

  get nameError(): string | null {
    const c = this.form.controls.name;
    if (!c.dirty || !c.errors) return null;
    if (c.errors['required']) return 'Le nom est obligatoire';
    if (c.errors['minlength']) return 'Minimum 3 caractères';
    if (c.errors['pattern']) return 'Lettres minuscules, chiffres et tirets uniquement';
    return null;
  }

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
    const { name, description } = this.form.getRawValue();
    try {
      await this.groupService.createGroup({ userId: this.targetUserId() ?? user?.id ?? 1, name: name.trim(), description: description || undefined });
      this.created.emit();
      this.close();
      this.form.reset({ name: '', description: '' });
    } catch {
      this.error.set(this.groupService.error() || 'Erreur lors de la création du groupe');
    }
  }
}
