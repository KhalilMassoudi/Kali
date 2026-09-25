package kali.microservices.infrastructureservice.dto;

import kali.microservices.infrastructureservice.openstack.PlatformTotals;

/**
 * Compact, pre-aggregated Gnocchi summary — the frontend gets one small object instead of
 * looping N per-VM telemetry calls itself. `dataAvailable=false` covers both "Gnocchi isn't
 * configured" and "configured but no samples yet for these VMs" — the two cases where showing
 * a bare 0 would be misleading rather than informative.
 *
 * `platform` is only populated on the fleet-wide (admin) summary — it's read live from
 * OpenStack (Nova/Cinder/Neutron limits, same source Horizon's own Overview page uses),
 * so it stays accurate even for resources created outside this platform, unlike vmCount/
 * totalVcpu/etc above which only reflect what our own tracking tables know about.
 */
public record MetricsSummary(
        int vmCount,
        int runningCount,
        int totalVcpu,
        int totalRamMb,
        int totalStorageGb,
        Double avgCpuUtil,
        boolean dataAvailable,
        PlatformTotals platform
) {
}
