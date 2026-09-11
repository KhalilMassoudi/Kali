import { Injectable, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { firstValueFrom } from 'rxjs';
import { MetricsSummary } from '../models/vm.model';

@Injectable({ providedIn: 'root' })
export class MetricsService {
  private readonly http = inject(HttpClient);

  private readonly _mySummary = signal<MetricsSummary | null>(null);
  private readonly _fleetSummary = signal<MetricsSummary | null>(null);

  readonly mySummary = this._mySummary.asReadonly();
  readonly fleetSummary = this._fleetSummary.asReadonly();

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
}
