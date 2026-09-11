import { Injectable, signal } from '@angular/core';

export type LandingPage = '/dashboard' | '/vms';
export type TableDensity = 'comfortable' | 'compact';

interface StoredPreferences {
  landingPage: LandingPage;
  tableDensity: TableDensity;
}

const STORAGE_KEY = 'safozi.preferences';

const DEFAULTS: StoredPreferences = {
  landingPage: '/dashboard',
  tableDensity: 'comfortable',
};

function load(): StoredPreferences {
  try {
    const raw = localStorage.getItem(STORAGE_KEY);
    if (!raw) return DEFAULTS;
    return { ...DEFAULTS, ...JSON.parse(raw) };
  } catch {
    return DEFAULTS;
  }
}

@Injectable({ providedIn: 'root' })
export class PreferencesService {
  private readonly initial = load();
  private readonly _landingPage = signal<LandingPage>(this.initial.landingPage);
  private readonly _tableDensity = signal<TableDensity>(this.initial.tableDensity);

  readonly landingPage = this._landingPage.asReadonly();
  readonly tableDensity = this._tableDensity.asReadonly();

  setLandingPage(value: LandingPage): void {
    this._landingPage.set(value);
    this.persist();
  }

  setTableDensity(value: TableDensity): void {
    this._tableDensity.set(value);
    this.persist();
  }

  private persist(): void {
    const data: StoredPreferences = { landingPage: this._landingPage(), tableDensity: this._tableDensity() };
    localStorage.setItem(STORAGE_KEY, JSON.stringify(data));
  }
}
