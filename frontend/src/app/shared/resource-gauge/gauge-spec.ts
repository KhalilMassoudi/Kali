export interface GaugeSpec {
  label: string;
  used: number;
  /** null when there's no limit to compare against (unlimited, or unknown). */
  limit: number | null;
  center: string;
  detail: string;
}

export interface GaugeGroup {
  title: string;
  gauges: GaugeSpec[];
}

/**
 * Builds one Horizon-style gauge: "3/10" in the ring, "Utilisé 3 sur 10" underneath.
 * A negative or null limit means unlimited/unknown, so only the used value is shown.
 */
export function gauge(label: string, used: number, limit: number | null, unit = '', divisor = 1): GaugeSpec {
  const u = Math.round(used / divisor);
  const l = limit !== null && limit >= 0 ? Math.round(limit / divisor) : null;
  return {
    label,
    used: u,
    limit: l,
    center: l !== null ? `${u}/${l}` : `${u}`,
    detail: l !== null ? `Utilisé ${u}${unit} sur ${l}${unit}` : `Utilisé ${u}${unit}`,
  };
}
