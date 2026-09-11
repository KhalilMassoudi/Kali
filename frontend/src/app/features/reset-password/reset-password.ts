import { Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { AbstractControl, FormBuilder, ReactiveFormsModule, ValidationErrors, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { PasswordModule } from 'primeng/password';
import { ButtonModule } from 'primeng/button';
import { MessageModule } from 'primeng/message';
import { AuthService } from '../../core/services/auth.service';

function passwordsMatch(control: AbstractControl): ValidationErrors | null {
  const next = control.get('next')?.value;
  const confirm = control.get('confirm')?.value;
  return confirm && next !== confirm ? { mismatch: true } : null;
}

@Component({
  selector: 'app-reset-password',
  imports: [CommonModule, ReactiveFormsModule, RouterLink, PasswordModule, ButtonModule, MessageModule],
  templateUrl: './reset-password.html',
  styleUrl: './reset-password.scss',
})
export class ResetPassword {
  private readonly fb = inject(FormBuilder);
  private readonly auth = inject(AuthService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);

  private readonly token = this.route.snapshot.queryParamMap.get('token');

  readonly submitting = signal(false);
  readonly succeeded = signal(false);
  readonly apiError = signal('');

  readonly form = this.fb.nonNullable.group(
    {
      next: ['', [Validators.required, Validators.minLength(6)]],
      confirm: ['', [Validators.required]],
    },
    { validators: passwordsMatch },
  );

  get nextError(): string | null {
    const c = this.form.controls.next;
    if (!c.touched || !c.errors) return null;
    if (c.errors['required']) return 'Nouveau mot de passe requis';
    if (c.errors['minlength']) return 'Minimum 6 caractères';
    return null;
  }

  get confirmError(): string | null {
    const c = this.form.controls.confirm;
    if (!c.touched) return null;
    if (this.form.errors?.['mismatch']) return 'Les mots de passe ne correspondent pas';
    return null;
  }

  async submit(): Promise<void> {
    this.apiError.set('');
    if (!this.token) {
      this.apiError.set('Lien de réinitialisation invalide');
      return;
    }
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.submitting.set(true);
    try {
      const { next } = this.form.getRawValue();
      await this.auth.resetPassword(this.token, next);
      this.succeeded.set(true);
    } catch {
      this.apiError.set(this.auth.error() || 'Lien invalide ou expiré');
    } finally {
      this.submitting.set(false);
    }
  }

  goToLogin(): void {
    this.router.navigate(['/']);
  }
}