export interface Wallet {
  id: number;
  userId: number;
  balance: number;
  currency: string;
  updatedAt: string;
}

export type WalletTransactionType = 'RECHARGE' | 'USAGE_DEDUCTION' | 'ADMIN_ADJUSTMENT';

export interface WalletTransaction {
  id: number;
  userId: number;
  type: WalletTransactionType;
  amount: number;
  balanceAfter: number;
  description: string | null;
  resourceType: string | null;
  resourceId: number | null;
  createdAt: string;
}

export interface PricingConfig {
  id: number;
  pricePerVcpuHour: number;
  pricePerRamGbHour: number;
  pricePerStorageGbHour: number;
  pricePerFloatingIpHour: number;
  currency: string;
  lowBalanceThreshold: number;
  suspendVmsOnZeroBalance: boolean;
  updatedAt: string;
}

export interface AlertThreshold {
  threshold: number;
  currency: string;
}

export interface MonthlyInvoiceSummary {
  month: string; // yyyy-MM
  invoiceNumber: string;
  openingBalance: number;
  usageTotal: number;
  rechargeTotal: number;
  adjustmentTotal: number;
  closingBalance: number;
  currency: string;
  current: boolean;
}

/** "2026-09" -> "Septembre 2026" (no French locale data is registered for the date pipe). */
export function invoiceMonthLabel(month: string): string {
  const [year, m] = month.split('-').map(Number);
  const label = new Intl.DateTimeFormat('fr-FR', { month: 'long', year: 'numeric' }).format(new Date(year, m - 1, 1));
  return label.charAt(0).toUpperCase() + label.slice(1);
}

export interface AdminAdjustRequest {
  amount: number;
  description?: string;
}
