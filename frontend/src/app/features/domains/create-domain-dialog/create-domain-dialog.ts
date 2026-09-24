import { Component, EventEmitter, Output, inject, input, model, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { DialogModule } from 'primeng/dialog';
import { InputTextModule } from 'primeng/inputtext';
import { ButtonModule } from 'primeng/button';
import { MessageModule } from 'primeng/message';
import { AuthService } from '../../../core/services/auth.service';
import { DomainService } from '../../../core/services/domain.service';

@Component({
  selector: 'app-create-domain-dialog',
  imports: [CommonModule, ReactiveFormsModule, DialogModule, InputTextModule, ButtonModule, MessageModule],
  templateUrl: './create-domain-dialog.html',
  styleUrl: './create-domain-dialog.scss',
})
export class CreateDomainDialog {
  private readonly fb = inject(FormBuilder);
  private readonly auth = inject(AuthService);
  private readonly domainService = inject(DomainService);

  readonly visible = model.required<boolean>();
  readonly targetUserId = input<number | null>(null);
  @Output() readonly created = new EventEmitter<void>();

  readonly loading = this.domainService.loading;
  readonly error = signal('');

  readonly form = this.fb.nonNullable.group({
    name: ['', [Validators.required, Validators.pattern(/^[a-z0-9-]+\.[a-z]{2,}$/)]],
  });

  get nameError(): string | null {
    const c = this.form.controls.name;
    if (!c.dirty || !c.errors) return null;
    if (c.errors['required']) return 'Le nom de domaine est obligatoire';
    if (c.errors['pattern']) return 'Format attendu: monsite.com';
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
    const { name } = this.form.getRawValue();
    try {
      await this.domainService.createDomain({ userId: this.targetUserId() ?? user?.id ?? 1, name: name.trim() });
      this.created.emit();
      this.close();
      this.form.reset({ name: '' });
    } catch {
      this.error.set(this.domainService.error() || 'Erreur lors de la création du domaine');
    }
  }
}
