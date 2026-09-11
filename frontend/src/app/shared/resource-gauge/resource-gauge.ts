import { Component, computed, input } from '@angular/core';
import { CommonModule } from '@angular/common';

const SIZE = 96;
const STROKE = 9;
const RADIUS = (SIZE - STROKE) / 2;
const CIRCUMFERENCE = 2 * Math.PI * RADIUS;

@Component({
  selector: 'app-resource-gauge',
  imports: [CommonModule],
  templateUrl: './resource-gauge.html',
  styleUrl: './resource-gauge.scss',
})
export class ResourceGauge {
  readonly label = input.required<string>();
  readonly used = input.required<number>();
  readonly total = input<number | null>(null);
  /** Free-text override for the center value (defaults to "used/total" or just "used"). */
  readonly displayValue = input<string | null>(null);
  readonly color = input<string>('var(--brand-accent, #2563eb)');

  readonly size = SIZE;
  readonly radius = RADIUS;
  readonly circumference = CIRCUMFERENCE;

  readonly pct = computed(() => {
    const total = this.total();
    if (!total || total <= 0) return 0;
    return Math.min(100, Math.max(0, (this.used() / total) * 100));
  });

  readonly dashOffset = computed(() => CIRCUMFERENCE - (this.pct() / 100) * CIRCUMFERENCE);

  readonly centerText = computed(() => {
    if (this.displayValue()) return this.displayValue();
    const total = this.total();
    return total !== null ? `${this.used()}/${total}` : `${this.used()}`;
  });
}
