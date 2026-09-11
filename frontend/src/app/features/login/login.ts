import { Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { InputTextModule } from 'primeng/inputtext';
import { PasswordModule } from 'primeng/password';
import { ButtonModule } from 'primeng/button';
import { MessageModule } from 'primeng/message';
import { AuthService } from '../../core/services/auth.service';
import { PreferencesService } from '../../core/services/preferences.service';

@Component({
  selector: 'app-login',
  imports: [CommonModule, ReactiveFormsModule, RouterLink, InputTextModule, PasswordModule, ButtonModule, MessageModule],
  templateUrl: './login.html',
  styleUrl: './login.scss',
})
export class Login {
  private readonly fb = inject(FormBuilder);
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  private readonly preferences = inject(PreferencesService);

  readonly loading = this.auth.loading;
  readonly apiError = signal('');
  readonly requiresTwoFactor = this.auth.requiresTwoFactor;

  readonly form = this.fb.nonNullable.group({
    email: ['', [Validators.required, Validators.email]],
    password: ['', [Validators.required]],
  });

  readonly codeForm = this.fb.nonNullable.group({
    code: ['', [Validators.required, Validators.minLength(6), Validators.maxLength(6)]],
  });

  get emailError(): string | null {
    const c = this.form.controls.email;
    if (!c.touched || !c.errors) return null;
    if (c.errors['required']) return 'Email requis';
    if (c.errors['email']) return 'Format invalide';
    return null;
  }

  get passwordError(): string | null {
    const c = this.form.controls.password;
    if (!c.touched || !c.errors) return null;
    if (c.errors['required']) return 'Mot de passe requis';
    return null;
  }

  async submit(): Promise<void> {
    this.apiError.set('');
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const { email, password } = this.form.getRawValue();
    try {
      await this.auth.login(email, password);
      if (!this.auth.requiresTwoFactor()) {
        this.router.navigate([this.preferences.landingPage()]);
      }
    } catch {
      this.apiError.set(this.auth.error() || 'Email ou mot de passe incorrect');
    }
  }

  async submitCode(): Promise<void> {
    this.apiError.set('');
    if (this.codeForm.invalid) {
      this.codeForm.markAllAsTouched();
      return;
    }
    const { code } = this.codeForm.getRawValue();
    try {
      await this.auth.verifyTwoFactorLogin(code);
      this.router.navigate([this.preferences.landingPage()]);
    } catch {
      this.apiError.set(this.auth.error() || 'Code invalide');
    }
  }

  backToCredentials(): void {
    this.auth.cancelTwoFactorLogin();
    this.codeForm.reset();
    this.apiError.set('');
  }
}
