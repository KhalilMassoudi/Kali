export type SmtpEncryption = 'NONE' | 'STARTTLS' | 'SSL';

export interface SmtpConfig {
  host: string | null;
  port: number | null;
  username: string | null;
  passwordConfigured: boolean;
  fromAddress: string | null;
  fromName: string | null;
  authEnabled: boolean;
  encryption: SmtpEncryption;
  updatedAt: string | null;
}

export interface UpdateSmtpConfigRequest {
  host: string;
  port: number;
  username?: string;
  /** Left blank to keep the currently stored password unchanged. */
  password?: string;
  fromAddress: string;
  fromName?: string;
  authEnabled: boolean;
  encryption: SmtpEncryption;
}

export interface AdminUser {
  id: number;
  email: string;
  firstName: string | null;
  lastName: string | null;
  role: string;
  enabled: boolean;
  createdAt: string | null;
}

export type AlertSeverity = 'INFO' | 'WARNING' | 'CRITICAL';
export type AlertType = 'CPU_HIGH' | 'MEMORY_HIGH' | 'RESPONSE_TIME_HIGH' | 'SERVICE_DOWN' | 'ERROR_RATE_HIGH';

export interface AlertEvent {
  id: number;
  ruleId: number;
  serviceName: string;
  alertType: AlertType;
  message: string;
  severity: AlertSeverity;
  firedAt: string;
  resolved: boolean;
  resolvedAt: string | null;
}

export interface AlertRule {
  id: number;
  userId: number;
  serviceName: string | null;
  alertType: AlertType;
  threshold: number;
  enabled: boolean;
  createdAt: string | null;
}

export interface CreateAlertRuleRequest {
  serviceName?: string | null;
  alertType: AlertType;
  threshold: number;
}

export interface ApiLogEntry {
  id: number;
  method: string;
  path: string;
  targetService: string | null;
  userEmail: string | null;
  userRole: string | null;
  statusCode: number | null;
  durationMs: number | null;
  timestamp: string;
}

export interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
}

export interface ApiLogFilters {
  service?: string;
  errorsOnly?: boolean;
  search?: string;
  page?: number;
  size?: number;
}

export interface TicketAgent {
  id: number;
  userId: number;
  grantedByUserId: number;
  grantedAt: string | null;
}

