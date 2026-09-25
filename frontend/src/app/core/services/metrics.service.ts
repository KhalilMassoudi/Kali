import { Injectable, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { firstValueFrom } from 'rxjs';
import { MetricsSummary, PlatformTotals } from '../models/vm.model';

@Injectable({ providedIn: 'root' })
export class MetricsService {
  private readonly http = inject(HttpClient);

  private readonly _mySummary = signal<MetricsSummary | null>(null);
  private readonly _fleetSummary = signal<MetricsSummary | null>(null);

  readonly mySummary = this._mySummary.asReadonly();
  readonly fleetSummary = this._fleetSummary.asReadonly();

  private readonly _platformTotals = signal<PlatformTotals | null>(null);
  private readonly _platformLoading = signal(false);
  readonly platformTotals = this._platformTotals.asReadonly();
  /** True while a request is in flight — the UI must not treat "null" as "unavailable" yet. */
  readonly platformLoading = this._platformLoading.asReadonly();

  async loadMySummary(): Promise<void> {
    try {
      const summary = await firstValueFrom(this.http.get<MetricsSummary>('/api/infrastructure/metrics/summary'));
      this._mySummary.set(summary);
    } catch {
      this._mySummary.set(null);
    }
  }

  async loadFleetSummary(): Promise<void> {
    try {
      const summary = await firstValueFrom(this.http.get<MetricsSummary>('/api/infrastructure/admin/metrics/summary'));
      this._fleetSummary.set(summary);
    } catch {
      this._fleetSummary.set(null);
    }
  }

  async loadPlatformTotals(): Promise<void> {
    this._platformLoading.set(true);
    try {
      // 204 (OpenStack unreachable, nothing cached) arrives as a null body.
      const totals = await firstValueFrom(this.http.get<PlatformTotals | null>('/api/infrastructure/admin/metrics/platform'));
      this._platformTotals.set(totals ?? null);
    } catch {
      this._platformTotals.set(null);
    } finally {
      this._platformLoading.set(false);
    }
  }
}
