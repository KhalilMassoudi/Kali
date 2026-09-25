package kali.microservices.infrastructureservice.dto;

/**
 * Compact, pre-aggregated Gnocchi summary — the frontend gets one small object instead of
 * looping N per-VM telemetry calls itself. `dataAvailable=false` covers both "Gnocchi isn't
 * configured" and "configured but no samples yet for these VMs" — the two cases where showing
 * a bare 0 would be misleading rather than informative.
 */
public record MetricsSummary(
        int vmCount,
        int runningCount,
        int totalVcpu,
        int totalRamMb,
        int totalStorageGb,
        Double avgCpuUtil,
        boolean dataAvailable
) {
}
