import { MeasurePoint } from '../../core/models/telemetry.model';

/**
 * Converts raw Gnocchi "cpu" measures (cumulative nanoseconds of CPU time since boot) into a
 * % utilization curve. This deployment's Ceilometer pipeline has no pre-computed "cpu_util"
 * metric — only the raw cumulative counter — so every consumer derives the rate itself from
 * consecutive samples: delta_ns / (granularity_seconds * 1e9 * vcpus).
 */
export function cumulativeCpuToUtilPercent(raw: MeasurePoint[], vcpus: number): MeasurePoint[] {
  const points: MeasurePoint[] = [];
  for (let i = 1; i < raw.length; i++) {
    const [, granularitySeconds, prevValue] = raw[i - 1];
    const [timestamp, , value] = raw[i];
    const deltaNs = value - prevValue;
    const windowNs = granularitySeconds * 1_000_000_000 * Math.max(1, vcpus);
    if (windowNs <= 0 || deltaNs < 0) continue;
    const percent = Math.min(100, Math.max(0, (deltaNs / windowNs) * 100));
    points.push([timestamp, granularitySeconds, percent]);
  }
  return points;
}
