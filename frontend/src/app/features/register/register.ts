import { Component, computed, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { AbstractControl, FormBuilder, ReactiveFormsModule, ValidationErrors, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { InputTextModule } from 'primeng/inputtext';
import { PasswordModule } from 'primeng/password';
import { ButtonModule } from 'primeng/button';
import { MessageModule } from 'primeng/message';
import { AuthService } from '../../core/services/auth.service';
import { PreferencesService } from '../../core/services/preferences.service';

function passwordsMatch(control: AbstractControl): ValidationErrors | null {
  const password = control.get('password')?.value;
  const confirm = control.get('confirm')?.value;
  return confirm && password !== confirm ? { mismatch: true } : null;
}

@Component({
  selector: 'app-register',
  imports: [CommonModule, ReactiveFormsModule, RouterLink, InputTextModule, PasswordModule, ButtonModule, MessageModule],
  templateUrl: './register.html',
  styleUrl: './register.scss',
})
export class Register {
  private readonly fb = inject(FormBuilder);
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  private readonly preferences = inject(PreferencesService);

  readonly loading = this.auth.loading;
  readonly apiError = signal('');

  readonly form = this.fb.nonNullable.group(
    {
      firstName: ['', [Validators.required]],
      lastName: [''],
      email: ['', [Validators.required, Validators.email]],
      password: ['', [Validators.required, Validators.minLength(6)]],
      confirm: [''],
    },
    { validators: passwordsMatch },
  );

  readonly passwordValue = computed(() => this.form.controls.password.value ?? '');

  readonly strength = computed(() => {
    const len = this.passwordValue().length;
    if (len === 0) return 0;
    if (len < 6) return 1;
    if (len < 10) return 2;
    return 3;
  });

  readonly strengthLabel = computed(() => ['', 'Faible', 'Correct', 'Fort'][this.strength()]);

  get firstNameError(): string | null {
    const c = this.form.controls.firstName;
    return c.touched && c.errors?.['required'] ? 'Prénom requis' : null;
  }

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
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const { email, password, firstName, lastName } = this.form.getRawValue();
    try {
      await this.auth.register(email, password, firstName, lastName);
      this.router.navigate([this.preferences.landingPage()]);
    } catch {
      this.apiError.set(this.auth.error() || "Erreur lors de l'inscription");
    }
  }
}
