import { Component, computed, effect, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ButtonModule } from 'primeng/button';
import { AuthService } from '../../../core/services/auth.service';
import { WalletService } from '../../../core/services/wallet.service';
import { USAGE_TYPE_LABELS, UsageSummary } from '../../../core/models/billing.model';

type Granularity = 'day' | 'week';

interface Bucket {
  key: string;
  label: string;
  total: number;
  byType: Record<string, number>;
}

const dayFmt = new Intl.DateTimeFormat('fr-FR', { day: 'numeric', month: 'short' });
const longFmt = new Intl.DateTimeFormat('fr-FR', { day: 'numeric', month: 'long', year: 'numeric' });

/** "2026-09-28" parsed as a local date (not UTC midnight, which can shift the day). */
function parseDay(iso: string): Date {
  const [y, m, d] = iso.split('-').map(Number);
  return new Date(y, m - 1, d);
}

function mondayOf(d: Date): Date {
  const r = new Date(d);
  r.setDate(r.getDate() - ((r.getDay() + 6) % 7));
  return r;
}

@Component({
  selector: 'app-billing-usage',
  imports: [CommonModule, ButtonModule],
  templateUrl: './billing-usage.html',
  styleUrl: './billing-usage.scss',
})
export class BillingUsage {
  private readonly auth = inject(AuthService);
  private readonly walletService = inject(WalletService);

  readonly days = signal<7 | 30>(30);
  readonly granularity = signal<Granularity>('day');
  readonly typeFilter = signal<string>('ALL');
  readonly summary = signal<UsageSummary | null>(null);
  readonly loading = signal(false);
  readonly error = signal<string | null>(null);

  readonly typeLabel = (t: string) => USAGE_TYPE_LABELS[t] ?? t;
  readonly currency = computed(() => this.summary()?.currency ?? 'TND');

  readonly periodLabel = computed(() => {
    const s = this.summary();
    return s ? `du ${longFmt.format(parseDay(s.from))} au ${longFmt.format(parseDay(s.to))}` : '';
  });

  /** Resource types that actually cost something in the window, most expensive first. */
  readonly types = computed(() => {
    const byType = this.summary()?.byType ?? {};
    return Object.entries(byType)
      .filter(([, v]) => v > 0)
      .sort((a, b) => b[1] - a[1])
      .map(([type, amount]) => ({ type, amount }));
  });

  readonly byService = computed(() => {
    const total = this.summary()?.total ?? 0;
    return this.types().map((t) => ({ ...t, pct: total > 0 ? (t.amount / total) * 100 : 0 }));
  });

  readonly buckets = computed<Bucket[]>(() => {
    const s = this.summary();
    if (!s) return [];
    const filter = this.typeFilter();
    const pick = (d: { total: number; byType: Record<string, number> }) =>
      filter === 'ALL' ? d.total : (d.byType[filter] ?? 0);

    if (this.granularity() === 'day') {
      return s.daily.map((d) => ({ key: d.date, label: dayFmt.format(parseDay(d.date)), total: pick(d), byType: d.byType }));
    }
    const weeks = new Map<string, Bucket>();
    for (const d of s.daily) {
      const monday = mondayOf(parseDay(d.date));
      const key = monday.toISOString().slice(0, 10);
      const b = weeks.get(key) ?? { key, label: `Sem. du ${dayFmt.format(monday)}`, total: 0, byType: {} };
      b.total += pick(d);
      for (const [t, v] of Object.entries(d.byType)) b.byType[t] = (b.byType[t] ?? 0) + v;
      weeks.set(key, b);
    }
    return [...weeks.values()];
  });

  readonly maxBucket = computed(() => Math.max(0, ...this.buckets().map((b) => b.total)));

  /** Most recent period first, like a statement. */
  readonly breakdown = computed(() => [...this.buckets()].reverse());

  constructor() {
    effect(() => {
      const user = this.auth.user();
      const days = this.days();
      if (user) this.load(user.id, days);
    });
  }

  /** Row total across all types (the chart bars follow the chip filter, the table doesn't). */
  rowTotal(b: Bucket): number {
    return Object.values(b.byType).reduce((a, v) => a + v, 0);
  }

  barHeight(b: Bucket): number {
    const max = this.maxBucket();
    return max > 0 ? Math.max(2, (b.total / max) * 100) : 0;
  }

  /** Show every label for weeks; for 30 days, one label every 5 days keeps the axis readable. */
  showLabel(i: number): boolean {
    const n = this.buckets().length;
    return this.granularity() === 'week' || n <= 10 || i % 5 === 0 || i === n - 1;
  }

  setDays(days: 7 | 30): void {
    this.days.set(days);
  }

  private async load(userId: number, days: 7 | 30): Promise<void> {
    this.loading.set(true);
    this.error.set(null);
    try {
      const summary = await this.walletService.fetchUsage(userId, days);
      this.summary.set(summary);
      if (this.typeFilter() !== 'ALL' && !(summary.byType[this.typeFilter()] > 0)) this.typeFilter.set('ALL');
    } catch (err: any) {
      this.error.set(err?.error?.message || 'Erreur lors du chargement de la consommation');
    } finally {
      this.loading.set(false);
    }
  }

  exportCsv(): void {
    const s = this.summary();
    if (!s) return;
    const types = this.types().map((t) => t.type);
    const header = ['date', 'total', ...types.map((t) => this.typeLabel(t))];
    const rows = s.daily.map((d) => [d.date, d.total.toFixed(4), ...types.map((t) => (d.byType[t] ?? 0).toFixed(4))]);
    const csv = [header, ...rows].map((r) => r.join(';')).join('\n');
    const url = URL.createObjectURL(new Blob([csv], { type: 'text/csv;charset=utf-8' }));
    const link = document.createElement('a');
    link.href = url;
    link.download = `consommation-${s.from}-${s.to}.csv`;
    link.click();
    URL.revokeObjectURL(url);
  }
}
