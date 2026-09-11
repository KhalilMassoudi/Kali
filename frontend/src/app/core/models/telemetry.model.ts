export interface TelemetryResource {
  id: string;
  type: string;
  project_id?: string;
  metrics: Record<string, string>;
  [key: string]: unknown;
}

export interface TelemetryResourcesResponse {
  configured: boolean;
  resources: TelemetryResource[];
  error?: string;
}

/** [timestamp, granularitySeconds, value] */
export type MeasurePoint = [string, number, number];
