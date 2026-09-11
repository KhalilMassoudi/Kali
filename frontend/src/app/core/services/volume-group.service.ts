import { Injectable, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { firstValueFrom } from 'rxjs';

@Injectable({ providedIn: 'root' })
export class VolumeGroupService {
  private readonly http = inject(HttpClient);

  private readonly _supported = signal<boolean | null>(null); // null = not probed yet

  readonly supported = this._supported.asReadonly();

  /** Feature-detect probe — many OpenStack deployments don't enable Cinder generic volume groups. */
  async checkSupported(): Promise<boolean> {
    try {
      const result = await firstValueFrom(this.http.get<{ supported: boolean }>('/api/infrastructure/volume-groups/supported'));
      this._supported.set(result.supported);
      return result.supported;
    } catch {
      this._supported.set(false);
      return false;
    }
  }
}
