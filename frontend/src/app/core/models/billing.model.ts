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
  updatedAt: string;
}

export interface AdminAdjustRequest {
  amount: number;
  description?: string;
}
