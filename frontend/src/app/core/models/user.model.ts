export interface User {
  id: number;
  email: string;
  firstName: string | null;
  lastName: string | null;
  role: string;
  apiKey: string | null;
  createdAt: string | null;
  enabled: boolean;
  twoFactorEnabled: boolean;
}

export interface AuthResponse {
  token?: string;
  role?: string;
  redirectTo?: string;
  requiresTwoFactor?: boolean;
  pendingToken?: string;
}

export interface TwoFactorSetupResponse {
  secret: string;
  qrCodeImage: string;
}

export interface LoginRequest {
  email: string;
  password: string;
}

export interface RegisterRequest {
  email: string;
  password: string;
  firstName?: string;
  lastName?: string;
}

export interface UpdateProfileRequest {
  firstName?: string;
  lastName?: string;
}

export interface ChangePasswordRequest {
  currentPassword: string;
  newPassword: string;
}

export interface ForgotPasswordRequest {
  email: string;
}

export interface ResetPasswordRequest {
  token: string;
  newPassword: string;
}
