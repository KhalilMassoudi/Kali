import { Component, computed, effect, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { AbstractControl, FormBuilder, ReactiveFormsModule, ValidationErrors, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { InputTextModule } from 'primeng/inputtext';
import { ButtonModule } from 'primeng/button';
import { TagModule } from 'primeng/tag';
import { TabsModule } from 'primeng/tabs';
import { SelectModule } from 'primeng/select';
import { TooltipModule } from 'primeng/tooltip';
import { AuthService } from '../../core/services/auth.service';
import { LandingPage, PreferencesService, TableDensity } from '../../core/services/preferences.service';
import { TwoFactorSetupResponse } from '../../core/models/user.model';

function passwordsMatch(control: AbstractControl): ValidationErrors | null {
  const next = control.get('next')?.value;
  const confirm = control.get('confirm')?.value;
  return confirm && next !== confirm ? { mismatch: true } : null;
}

/** Decodes a JWT payload for display purposes only — not a signature check. */
function decodeJwtPayload(token: string): Record<string, unknown> | null {
  try {
    const payload = token.split('.')[1];
    const normalized = payload.replace(/-/g, '+').replace(/_/g, '/');
    const json = decodeURIComponent(
      atob(normalized)
        .split('')
        .map((c) => '%' + c.charCodeAt(0).toString(16).padStart(2, '0'))
        .join(''),
    );
    return JSON.parse(json);
  } catch {
    return null;
  }
}

@Component({
  selector: 'app-settings',
  imports: [CommonModule, ReactiveFormsModule, InputTextModule, ButtonModule, TagModule, TabsModule, SelectModule, TooltipModule],
  templateUrl: './settings.html',
  styleUrl: './settings.scss',
})
export class Settings {
  private readonly fb = inject(FormBuilder);
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  readonly preferences = inject(PreferencesService);

  readonly user = this.auth.user;

  readonly savingProfile = signal(false);
  readonly savedProfile = signal(false);
  readonly profileError = signal('');

  readonly changingPassword = signal(false);
  readonly passwordChanged = signal(false);
  readonly passwordError = signal('');

  readonly regeneratingKey = signal(false);
  readonly confirmRegenerate = signal(false);
  readonly showApiKey = signal(false);
  readonly keyCopied = signal(false);

  readonly confirmDeactivate = signal(false);
  readonly deactivating = signal(false);
  readonly deactivateError = signal('');

  readonly twoFactorEnabled = computed(() => this.user()?.twoFactorEnabled ?? false);
  readonly show2faSetup = signal(false);
  readonly setupData = signal<TwoFactorSetupResponse | null>(null);
  readonly settingUp2fa = signal(false);
  readonly verifying2fa = signal(false);
  readonly setup2faError = signal('');

  readonly confirmDisable2fa = signal(false);
  readonly disabling2fa = signal(false);
  readonly disable2faError = signal('');

  readonly landingPageOptions: { label: string; value: LandingPage }[] = [
    { label: 'Dashboard', value: '/dashboard' },
    { label: 'Mes VMs', value: '/vms' },
  ];

  readonly densityOptions: { label: string; value: TableDensity }[] = [
    { label: 'Confortable', value: 'comfortable' },
    { label: 'Compacte', value: 'compact' },
  ];

  readonly displayName = computed(() => {
    const u = this.user();
    if (!u) return '';
    return `${u.firstName ?? ''} ${u.lastName ?? ''}`.trim();
  });

  readonly initials = computed(() =>
    this.displayName()
      .split(' ')
      .filter(Boolean)
      .map((w) => w[0])
      .join('')
      .toUpperCase()
      .slice(0, 2),
  );

  readonly maskedApiKey = computed(() => {
    const key = this.user()?.apiKey;
    if (!key) return '—';
    if (this.showApiKey()) return key;
    return `${key.slice(0, 8)}${'•'.repeat(Math.max(key.length - 12, 8))}${key.slice(-4)}`;
  });

  readonly session = computed(() => {
    const token = this.auth.getToken();
    if (!token) return null;
    const payload = decodeJwtPayload(token);
    if (!payload) return null;
    const iat = payload['iat'];
    const exp = payload['exp'];
    return {
      issuedAt: typeof iat === 'number' ? new Date(iat * 1000) : null,
      expiresAt: typeof exp === 'number' ? new Date(exp * 1000) : null,
      role: typeof payload['role'] === 'string' ? (payload['role'] as string) : null,
    };
  });

  readonly profileForm = this.fb.nonNullable.group({
    firstName: [''],
    lastName: [''],
  });

  readonly securityForm = this.fb.nonNullable.group(
    {
      current: ['', [Validators.required]],
      next: ['', [Validators.required, Validators.minLength(6)]],
      confirm: ['', [Validators.required]],
    },
    { validators: passwordsMatch },
  );

  readonly preferencesForm = this.fb.nonNullable.group({
    landingPage: this.preferences.landingPage(),
    tableDensity: this.preferences.tableDensity(),
  });

  readonly verify2faForm = this.fb.nonNullable.group({
    code: ['', [Validators.required]],
  });

  readonly disable2faForm = this.fb.nonNullable.group({
    password: ['', [Validators.required]],
    code: ['', [Validators.required]],
  });

  constructor() {
    effect(() => {
      const u = this.user();
      if (u) {
        this.profileForm.patchValue({ firstName: u.firstName ?? '', lastName: u.lastName ?? '' }, { emitEvent: false });
      }
    });

    this.preferencesForm.controls.landingPage.valueChanges.subscribe((v) => this.preferences.setLandingPage(v));
    this.preferencesForm.controls.tableDensity.valueChanges.subscribe((v) => this.preferences.setTableDensity(v));
  }

  get newPasswordError(): string | null {
    const c = this.securityForm.controls.next;
    if (!c.touched || !c.errors) return null;
    if (c.errors['required']) return 'Nouveau mot de passe requis';
    if (c.errors['minlength']) return 'Minimum 6 caractères';
    return null;
  }

  get confirmPasswordError(): string | null {
    const c = this.securityForm.controls.confirm;
    if (!c.touched) return null;
    if (this.securityForm.errors?.['mismatch']) return 'Les mots de passe ne correspondent pas';
    return null;
  }

  async saveProfile(): Promise<void> {
    this.profileError.set('');
    this.savingProfile.set(true);
    try {
      const { firstName, lastName } = this.profileForm.getRawValue();
      await this.auth.updateProfile({ firstName, lastName });
      this.savedProfile.set(true);
      setTimeout(() => this.savedProfile.set(false), 3000);
    } catch {
      this.profileError.set(this.auth.error() || 'Erreur lors de la mise à jour du profil');
    } finally {
      this.savingProfile.set(false);
    }
  }

  async changePassword(): Promise<void> {
    this.passwordError.set('');
    if (this.securityForm.invalid) {
      this.securityForm.markAllAsTouched();
      return;
    }
    this.changingPassword.set(true);
    try {
      const { current, next } = this.securityForm.getRawValue();
      await this.auth.changePassword({ currentPassword: current, newPassword: next });
      this.securityForm.reset();
      this.passwordChanged.set(true);
      setTimeout(() => this.passwordChanged.set(false), 3000);
    } catch {
      this.passwordError.set(this.auth.error() || 'Erreur lors du changement de mot de passe');
    } finally {
      this.changingPassword.set(false);
    }
  }

  toggleApiKeyVisibility(): void {
    this.showApiKey.update((v) => !v);
  }

  async copyApiKey(): Promise<void> {
    const key = this.user()?.apiKey;
    if (!key) return;
    await navigator.clipboard.writeText(key);
    this.keyCopied.set(true);
    setTimeout(() => this.keyCopied.set(false), 2000);
  }

  askRegenerate(): void {
    this.confirmRegenerate.set(true);
  }

  cancelRegenerate(): void {
    this.confirmRegenerate.set(false);
  }

  async regenerateApiKey(): Promise<void> {
    this.regeneratingKey.set(true);
    try {
      await this.auth.regenerateApiKey();
      this.showApiKey.set(true);
    } finally {
      this.regeneratingKey.set(false);
      this.confirmRegenerate.set(false);
    }
  }

  askDeactivate(): void {
    this.confirmDeactivate.set(true);
  }

  cancelDeactivate(): void {
    this.confirmDeactivate.set(false);
  }

  async deactivateAccount(): Promise<void> {
    this.deactivateError.set('');
    this.deactivating.set(true);
    try {
      await this.auth.deactivateAccount();
      this.router.navigateByUrl('/');
    } catch {
      this.deactivateError.set(this.auth.error() || 'Erreur lors de la désactivation du compte');
      this.deactivating.set(false);
    }
  }

  formatDate(date: Date | null): string {
    if (!date) return '—';
    return date.toLocaleString('fr-FR', { dateStyle: 'medium', timeStyle: 'short' });
  }

  async startSetup2fa(): Promise<void> {
    this.setup2faError.set('');
    this.settingUp2fa.set(true);
    try {
      const data = await this.auth.setup2fa();
      this.setupData.set(data);
      this.show2faSetup.set(true);
    } catch {
      this.setup2faError.set(this.auth.error() || 'Erreur lors de la configuration de la 2FA');
    } finally {
      this.settingUp2fa.set(false);
    }
  }

  async confirmSetup2fa(): Promise<void> {
    this.setup2faError.set('');
    if (this.verify2faForm.invalid) {
      this.verify2faForm.markAllAsTouched();
      return;
    }
    this.verifying2fa.set(true);
    try {
      const { code } = this.verify2faForm.getRawValue();
      await this.auth.verify2faSetup(code);
      this.show2faSetup.set(false);
      this.setupData.set(null);
      this.verify2faForm.reset();
    } catch {
      this.setup2faError.set(this.auth.error() || 'Code invalide');
    } finally {
      this.verifying2fa.set(false);
    }
  }

  cancelSetup2fa(): void {
    this.show2faSetup.set(false);
    this.setupData.set(null);
    this.verify2faForm.reset();
    this.setup2faError.set('');
  }

  askDisable2fa(): void {
    this.confirmDisable2fa.set(true);
  }

  cancelDisable2fa(): void {
    this.confirmDisable2fa.set(false);
    this.disable2faForm.reset();
    this.disable2faError.set('');
  }

  async disable2fa(): Promise<void> {
    this.disable2faError.set('');
    if (this.disable2faForm.invalid) {
      this.disable2faForm.markAllAsTouched();
      return;
    }
    this.disabling2fa.set(true);
    try {
      const { password, code } = this.disable2faForm.getRawValue();
      await this.auth.disable2fa(password, code);
      this.confirmDisable2fa.set(false);
      this.disable2faForm.reset();
    } catch {
      this.disable2faError.set(this.auth.error() || 'Erreur lors de la désactivation de la 2FA');
    } finally {
      this.disabling2fa.set(false);
    }
  }
}
