import { Injectable, signal } from '@angular/core';

export type Theme = 'dark' | 'light';

const STORAGE_KEY = 'safozi-theme';

/**
 * Light/dark toggle. The theme is applied as `data-theme` on <html> (our CSS tokens) plus the
 * `app-dark` class PrimeNG's darkModeSelector keys on. index.html applies the saved choice
 * before Angular boots so the page doesn't flash the wrong theme.
 */
@Injectable({ providedIn: 'root' })
export class ThemeService {
  private readonly _theme = signal<Theme>(this.initialTheme());
  readonly theme = this._theme.asReadonly();

  constructor() {
    this.apply(this._theme());
  }

  toggle(): void {
    this.set(this._theme() === 'dark' ? 'light' : 'dark');
  }

  set(theme: Theme): void {
    this._theme.set(theme);
    this.apply(theme);
    try {
      localStorage.setItem(STORAGE_KEY, theme);
    } catch {
      // Storage blocked (private mode) — the choice just won't persist across reloads.
    }
  }

  private initialTheme(): Theme {
    try {
      const saved = localStorage.getItem(STORAGE_KEY);
      if (saved === 'dark' || saved === 'light') return saved;
    } catch {
      // fall through to the default
    }
    return 'dark';
  }

  private apply(theme: Theme): void {
    const root = document.documentElement;
    root.dataset['theme'] = theme;
    root.classList.toggle('app-dark', theme === 'dark');
  }
}
