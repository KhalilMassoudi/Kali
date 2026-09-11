import { Injectable, computed, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { firstValueFrom } from 'rxjs';
import {
  AuthResponse,
  ChangePasswordRequest,
  ForgotPasswordRequest,
  LoginRequest,
  RegisterRequest,
  ResetPasswordRequest,
  TwoFactorSetupResponse,
  UpdateProfileRequest,
  User,
} from '../models/user.model';

const TOKEN_KEY = 'token';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);

  private readonly _user = signal<User | null>(null);
  private readonly _token = signal<string | null>(localStorage.getItem(TOKEN_KEY));
  private readonly _loading = signal(false);
  private readonly _error = signal<string | null>(null);
  private readonly _requiresTwoFactor = signal(false);
  private readonly _pendingToken = signal<string | null>(null);

  readonly user = this._user.asReadonly();
  readonly loading = this._loading.asReadonly();
  readonly error = this._error.asReadonly();
  readonly isLoggedIn = computed(() => !!this._token());
  readonly requiresTwoFactor = this._requiresTwoFactor.asReadonly();

  constructor() {
    if (this._token()) {
      this.getMe();
    }
  }

  getToken(): string | null {
    return this._token();
  }

  async login(email: string, password: string): Promise<void> {
    this._loading.set(true);
    this._error.set(null);
    this._requiresTwoFactor.set(false);
    try {
      const body: LoginRequest = { email, password };
      const res = await firstValueFrom(this.http.post<AuthResponse>('/api/auth/login', body));
      if (res.requiresTwoFactor) {
        this._pendingToken.set(res.pendingToken ?? null);
        this._requiresTwoFactor.set(true);
        return;
      }
      localStorage.setItem(TOKEN_KEY, res.token!);
      this._token.set(res.token!);
      await this.getMe();
    } catch (err: any) {
      this._error.set(err?.error?.message || 'Identifiants incorrects');
      throw err;
    } finally {
      this._loading.set(false);
    }
  }

  async verifyTwoFactorLogin(code: string): Promise<void> {
    const pendingToken = this._pendingToken();
    if (!pendingToken) {
      throw new Error('Session invalide, veuillez vous reconnecter');
    }
    this._loading.set(true);
    this._error.set(null);
    try {
      const res = await firstValueFrom(
        this.http.post<AuthResponse>('/api/auth/2fa/login-verify', { pendingToken, code }),
      );
      localStorage.setItem(TOKEN_KEY, res.token!);
      this._token.set(res.token!);
      this._requiresTwoFactor.set(false);
      this._pendingToken.set(null);
      await this.getMe();
    } catch (err: any) {
      this._error.set(err?.error?.message || 'Code invalide');
      throw err;
    } finally {
      this._loading.set(false);
    }
  }

  cancelTwoFactorLogin(): void {
    this._requiresTwoFactor.set(false);
    this._pendingToken.set(null);
  }

  async setup2fa(): Promise<TwoFactorSetupResponse> {
    this._error.set(null);
    try {
      return await firstValueFrom(this.http.post<TwoFactorSetupResponse>('/api/auth/2fa/setup', {}));
    } catch (err: any) {
      this._error.set(err?.error?.message || 'Erreur lors de la configuration de la 2FA');
      throw err;
    }
  }

  async verify2faSetup(code: string): Promise<void> {
    this._error.set(null);
    try {
      await firstValueFrom(this.http.post<void>('/api/auth/2fa/verify-setup', { code }));
      await this.getMe();
    } catch (err: any) {
      this._error.set(err?.error?.message || 'Code invalide');
      throw err;
    }
  }

  async disable2fa(password: string, code: string): Promise<void> {
    this._error.set(null);
    try {
      await firstValueFrom(this.http.post<void>('/api/auth/2fa/disable', { password, code }));
      await this.getMe();
    } catch (err: any) {
      this._error.set(err?.error?.message || 'Erreur lors de la désactivation de la 2FA');
      throw err;
    }
  }

  async register(email: string, password: string, firstName?: string, lastName?: string): Promise<void> {
    this._loading.set(true);
    this._error.set(null);
    try {
      const body: RegisterRequest = { email, password, firstName, lastName };
      const res = await firstValueFrom(this.http.post<AuthResponse>('/api/auth/register', body));
      localStorage.setItem(TOKEN_KEY, res.token!);
      this._token.set(res.token!);
      await this.getMe();
    } catch (err: any) {
      this._error.set(err?.error?.message || "Erreur lors de l'inscription");
      throw err;
    } finally {
      this._loading.set(false);
    }
  }

  async getMe(): Promise<void> {
    try {
      const user = await firstValueFrom(this.http.get<User>('/api/auth/me'));
      this._user.set(user);
    } catch {
      this.logout();
    }
  }

  async updateProfile(data: UpdateProfileRequest): Promise<void> {
    this._error.set(null);
    try {
      const user = await firstValueFrom(this.http.put<User>('/api/auth/me', data));
      this._user.set(user);
    } catch (err: any) {
      this._error.set(err?.error?.message || 'Erreur lors de la mise à jour du profil');
      throw err;
    }
  }

  async changePassword(data: ChangePasswordRequest): Promise<void> {
    this._error.set(null);
    try {
      await firstValueFrom(this.http.put<void>('/api/auth/password', data));
    } catch (err: any) {
      this._error.set(err?.error?.message || 'Erreur lors du changement de mot de passe');
      throw err;
    }
  }

  async regenerateApiKey(): Promise<void> {
    this._error.set(null);
    try {
      const user = await firstValueFrom(this.http.post<User>('/api/auth/api-key/regenerate', {}));
      this._user.set(user);
    } catch (err: any) {
      this._error.set(err?.error?.message || "Erreur lors de la régénération de la clé API");
      throw err;
    }
  }

  async deactivateAccount(): Promise<void> {
    this._error.set(null);
    try {
      await firstValueFrom(this.http.post<void>('/api/auth/deactivate', {}));
      this.logout();
    } catch (err: any) {
      this._error.set(err?.error?.message || 'Erreur lors de la désactivation du compte');
      throw err;
    }
  }

  async forgotPassword(email: string): Promise<void> {
    this._error.set(null);
    try {
      const body: ForgotPasswordRequest = { email };
      await firstValueFrom(this.http.post<void>('/api/auth/forgot-password', body));
    } catch (err: any) {
      this._error.set(err?.error?.message || "Erreur lors de l'envoi de l'email");
      throw err;
    }
  }

  async resetPassword(token: string, newPassword: string): Promise<void> {
    this._error.set(null);
    try {
      const body: ResetPasswordRequest = { token, newPassword };
      await firstValueFrom(this.http.post<void>('/api/auth/reset-password', body));
    } catch (err: any) {
      this._error.set(err?.error?.message || 'Erreur lors de la réinitialisation du mot de passe');
      throw err;
    }
  }

  logout(): void {
    localStorage.removeItem(TOKEN_KEY);
    this._token.set(null);
    this._user.set(null);
  }

  clearError(): void {
    this._error.set(null);
  }
}