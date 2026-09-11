import { Component, EventEmitter, Output, inject, input, model, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { DialogModule } from 'primeng/dialog';
import { InputTextModule } from 'primeng/inputtext';
import { ButtonModule } from 'primeng/button';
import { MessageModule } from 'primeng/message';
import { AuthService } from '../../../core/services/auth.service';
import { NetworkService } from '../../../core/services/network.service';

const CIDR_PATTERN = /^(\d{1,3}\.){3}\d{1,3}\/\d{1,2}$/;

@Component({
  selector: 'app-create-network-dialog',
  imports: [CommonModule, ReactiveFormsModule, DialogModule, InputTextModule, ButtonModule, MessageModule],
  templateUrl: './create-network-dialog.html',
  styleUrl: './create-network-dialog.scss',
})
export class CreateNetworkDialog {
  private readonly fb = inject(FormBuilder);
  private readonly auth = inject(AuthService);
  private readonly networkService = inject(NetworkService);

  readonly visible = model.required<boolean>();
  /** Set by admin pages to create the network on behalf of a chosen client instead of self. */
  readonly targetUserId = input<number | null>(null);
  @Output() readonly created = new EventEmitter<void>();

  readonly loading = this.networkService.loading;
  readonly error = signal('');

  readonly form = this.fb.nonNullable.group({
    name: ['', [Validators.required, Validators.minLength(3), Validators.pattern(/^[a-z0-9-]+$/)]],
    cidr: ['192.168.100.0/24', [Validators.required, Validators.pattern(CIDR_PATTERN)]],
  });

  get nameError(): string | null {
    const c = this.form.controls.name;
    if (!c.dirty || !c.errors) return null;
    if (c.errors['required']) return 'Le nom est obligatoire';
    if (c.errors['minlength']) return 'Minimum 3 caractères';
    if (c.errors['pattern']) return 'Lettres minuscules, chiffres et tirets uniquement';
    return null;
  }

  get cidrError(): string | null {
    const c = this.form.controls.cidr;
    if (!c.dirty || !c.errors) return null;
    if (c.errors['required']) return 'Le CIDR est obligatoire';
    if (c.errors['pattern']) return "Format attendu: 192.168.1.0/24";
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
    const { name, cidr } = this.form.getRawValue();
    try {
      await this.networkService.createNetwork({ userId: this.targetUserId() ?? user?.id ?? 1, name: name.trim(), cidr });
      this.created.emit();
      this.close();
      this.form.reset({ name: '', cidr: '192.168.100.0/24' });
    } catch {
      this.error.set(this.networkService.error() || 'Erreur lors de la création du réseau');
    }
  }
}
