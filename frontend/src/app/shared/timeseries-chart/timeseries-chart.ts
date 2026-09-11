import { Component, computed, input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MeasurePoint } from '../../core/models/telemetry.model';

const WIDTH = 640;
const HEIGHT = 220;
const PADDING = 32;

@Component({
  selector: 'app-timeseries-chart',
  imports: [CommonModule],
  templateUrl: './timeseries-chart.html',
  styleUrl: './timeseries-chart.scss',
})
export class TimeseriesChart {
  readonly points = input<MeasurePoint[]>([]);
  readonly unit = input<string>('');

  readonly width = WIDTH;
  readonly height = HEIGHT;

  private readonly sorted = computed(() =>
    [...this.points()].sort((a, b) => new Date(a[0]).getTime() - new Date(b[0]).getTime()),
  );

  readonly hasData = computed(() => this.sorted().length > 0);

  readonly minValue = computed(() => Math.min(...this.sorted().map((p) => p[2]), 0));
  readonly maxValue = computed(() => {
    const max = Math.max(...this.sorted().map((p) => p[2]), 0);
    return max === this.minValue() ? max + 1 : max;
  });

  readonly firstTimestamp = computed(() => this.sorted()[0]?.[0] ?? null);
  readonly lastTimestamp = computed(() => this.sorted().at(-1)?.[0] ?? null);
  readonly lastValue = computed(() => this.sorted().at(-1)?.[2] ?? null);

  readonly polylinePoints = computed(() => {
    const data = this.sorted();
    if (data.length === 0) return '';
    const t0 = new Date(data[0][0]).getTime();
    const t1 = new Date(data.at(-1)![0]).getTime();
    const timeSpan = t1 - t0 || 1;
    const min = this.minValue();
    const max = this.maxValue();
    const valueSpan = max - min || 1;

    return data
      .map((p) => {
        const t = new Date(p[0]).getTime();
        const x = PADDING + ((t - t0) / timeSpan) * (WIDTH - 2 * PADDING);
        const y = HEIGHT - PADDING - ((p[2] - min) / valueSpan) * (HEIGHT - 2 * PADDING);
        return `${x.toFixed(1)},${y.toFixed(1)}`;
      })
      .join(' ');
  });

  formatTimestamp(ts: string | null): string {
    if (!ts) return '';
    return new Date(ts).toLocaleString('fr-FR', { dateStyle: 'short', timeStyle: 'short' });
  }
}
