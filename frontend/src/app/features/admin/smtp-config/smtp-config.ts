import { Component, effect, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { InputTextModule } from 'primeng/inputtext';
import { InputNumberModule } from 'primeng/inputnumber';
import { ButtonModule } from 'primeng/button';
import { SelectModule } from 'primeng/select';
import { ToggleSwitchModule } from 'primeng/toggleswitch';
import { AdminService } from '../../../core/services/admin.service';
import { SmtpEncryption } from '../../../core/models/admin.model';

@Component({
  selector: 'app-smtp-config',
  imports: [CommonModule, ReactiveFormsModule, InputTextModule, InputNumberModule, ButtonModule, SelectModule, ToggleSwitchModule],
  templateUrl: './smtp-config.html',
  styleUrl: './smtp-config.scss',
})
export class SmtpConfigPage {
  private readonly fb = inject(FormBuilder);
  private readonly admin = inject(AdminService);

  readonly config = this.admin.smtpConfig;

  readonly loading = signal(true);
  readonly saving = signal(false);
  readonly savedOk = signal(false);
  readonly saveError = signal('');

  readonly testing = signal(false);
  readonly testError = signal('');
  readonly testOk = signal(false);

  readonly encryptionOptions: { label: string; value: SmtpEncryption }[] = [
    { label: 'Aucun', value: 'NONE' },
    { label: 'STARTTLS', value: 'STARTTLS' },
    { label: 'SSL/TLS', value: 'SSL' },
  ];

  readonly form = this.fb.nonNullable.group({
    host: ['', [Validators.required]],
    port: [587, [Validators.required]],
    username: [''],
    password: [''],
    fromAddress: ['', [Validators.required, Validators.email]],
    fromName: [''],
    authEnabled: [true],
    encryption: ['STARTTLS' as SmtpEncryption, [Validators.required]],
  });

  readonly testForm = this.fb.nonNullable.group({
    recipient: ['', [Validators.required, Validators.email]],
  });

  constructor() {
    effect(() => {
      const c = this.config();
      if (c) {
        this.form.patchValue(
          {
            host: c.host ?? '',
            port: c.port ?? 587,
            username: c.username ?? '',
            fromAddress: c.fromAddress ?? '',
            fromName: c.fromName ?? '',
            authEnabled: c.authEnabled,
            encryption: c.encryption,
          },
          { emitEvent: false },
        );
      }
    });

    this.load();
  }

  private async load(): Promise<void> {
    this.loading.set(true);
    try {
      await this.admin.loadSmtpConfig();
    } finally {
      this.loading.set(false);
    }
  }

  get hostError(): string | null {
    const c = this.form.controls.host;
    return c.touched && c.errors?.['required'] ? 'Hôte requis' : null;
  }

  get fromAddressError(): string | null {
    const c = this.form.controls.fromAddress;
    if (!c.touched || !c.errors) return null;
    if (c.errors['required']) return "Adresse d'expédition requise";
    if (c.errors['email']) return 'Format invalide';
    return null;
  }

  async save(): Promise<void> {
    this.saveError.set('');
    this.savedOk.set(false);
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.saving.set(true);
    try {
      const { host, port, username, password, fromAddress, fromName, authEnabled, encryption } = this.form.getRawValue();
      await this.admin.updateSmtpConfig({
        host,
        port,
        username: username || undefined,
        password: password || undefined,
        fromAddress,
        fromName: fromName || undefined,
        authEnabled,
        encryption,
      });
      this.form.patchValue({ password: '' }, { emitEvent: false });
      this.savedOk.set(true);
      setTimeout(() => this.savedOk.set(false), 3000);
    } catch {
      this.saveError.set(this.admin.error() || 'Erreur lors de la mise à jour de la configuration SMTP');
    } finally {
      this.saving.set(false);
    }
  }

  async sendTest(): Promise<void> {
    this.testError.set('');
    this.testOk.set(false);
    if (this.testForm.invalid) {
      this.testForm.markAllAsTouched();
      return;
    }
    this.testing.set(true);
    try {
      const { recipient } = this.testForm.getRawValue();
      await this.admin.sendTestEmail(recipient);
      this.testOk.set(true);
      setTimeout(() => this.testOk.set(false), 4000);
    } catch {
      this.testError.set(this.admin.error() || "Erreur lors de l'envoi de l'email de test");
    } finally {
      this.testing.set(false);
    }
  }
}