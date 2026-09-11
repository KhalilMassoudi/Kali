import { Injectable, inject, signal } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { firstValueFrom } from 'rxjs';
import { MeasurePoint, TelemetryResource, TelemetryResourcesResponse } from '../models/telemetry.model';

@Injectable({ providedIn: 'root' })
export class TelemetryService {
  private readonly http = inject(HttpClient);

  private readonly _resources = signal<TelemetryResource[]>([]);
  private readonly _configured = signal(true);
  private readonly _error = signal<string | null>(null);

  readonly resources = this._resources.asReadonly();
  readonly configured = this._configured.asReadonly();
  readonly error = this._error.asReadonly();

  async loadResources(type = 'generic'): Promise<void> {
    this._error.set(null);
    try {
      const res = await firstValueFrom(
        this.http.get<TelemetryResourcesResponse>('/api/infrastructure/telemetry/resources', {
          params: { type },
        }),
      );
      this._configured.set(res.configured);
      this._resources.set(res.resources ?? []);
      if (res.error) this._error.set(res.error);
    } catch (err: any) {
      this._error.set(err?.error?.message || 'Erreur lors du chargement des ressources de télémétrie');
      throw err;
    }
  }

  async getMeasures(resourceId: string, metricName: string, type = 'generic', granularity?: number): Promise<MeasurePoint[]> {
    let params = new HttpParams().set('type', type);
    if (granularity) params = params.set('granularity', granularity);
    return firstValueFrom(
      this.http.get<MeasurePoint[]>(
        `/api/infrastructure/telemetry/resources/${resourceId}/metrics/${metricName}/measures`,
        { params },
      ),
    );
  }

  clearError(): void {
    this._error.set(null);
  }
}
