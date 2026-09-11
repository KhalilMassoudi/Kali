package kali.microservices.infrastructureservice.service;

import kali.microservices.infrastructureservice.dto.MetricsSummary;
import kali.microservices.infrastructureservice.entities.VpsServer;
import kali.microservices.infrastructureservice.openstack.telemetry.GnocchiClient;
import kali.microservices.infrastructureservice.repository.VpsServerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Aggregates Gnocchi telemetry across a set of VMs into one compact summary, so the
 * dashboard/admin infra pages don't need to loop N per-VM calls themselves. The Gnocchi
 * resource id for an "instance" resource is the same as the Nova server UUID, i.e.
 * VpsServer.externalId — no extra mapping table needed.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MetricsAggregationService {

    /**
     * This deployment's Ceilometer pipeline only publishes the raw cumulative "cpu" counter
     * (nanoseconds of CPU time consumed since boot, sampled every archive-policy granularity)
     * — there is no "cpu_util" (already-computed percentage) metric anywhere in this Gnocchi
     * instance (confirmed by querying every instance resource directly). So utilization % is
     * derived here from the last two samples: delta_ns / (granularity_seconds * 1e9 * vcpus).
     */
    private static final String METRIC = "cpu";

    private final GnocchiClient gnocchiClient;
    private final VpsServerRepository vpsServerRepository;

    public MetricsSummary getSummaryForUser(Long userId) {
        List<VpsServer> vms = vpsServerRepository.findByUserId(userId).stream()
                .filter(v -> v.getStatus() != VpsServer.VpsStatus.DELETED)
                .toList();
        return summarize(vms);
    }

    public MetricsSummary getFleetSummary() {
        List<VpsServer> vms = vpsServerRepository.findAll().stream()
                .filter(v -> v.getStatus() != VpsServer.VpsStatus.DELETED)
                .toList();
        return summarize(vms);
    }

    private MetricsSummary summarize(List<VpsServer> vms) {
        int vmCount = vms.size();
        int runningCount = (int) vms.stream().filter(v -> v.getStatus() == VpsServer.VpsStatus.RUNNING).count();
        int totalVcpu = vms.stream().mapToInt(v -> v.getCpu() != null ? v.getCpu() : 0).sum();
        int totalRamMb = vms.stream().mapToInt(v -> v.getRam() != null ? v.getRam() : 0).sum();
        int totalStorageGb = vms.stream().mapToInt(v -> v.getStorage() != null ? v.getStorage() : 0).sum();

        if (!gnocchiClient.isConfigured()) {
            return new MetricsSummary(vmCount, runningCount, totalVcpu, totalRamMb, totalStorageGb, null, false);
        }

        double sum = 0;
        int samples = 0;
        for (VpsServer vm : vms) {
            if (vm.getExternalId() == null) continue;
            try {
                List<List<Object>> measures = gnocchiClient.getMeasures("instance", vm.getExternalId(), METRIC,
                        null, null, null);
                Double util = cpuUtilFromCumulativeMeasures(measures, vm.getCpu() != null ? vm.getCpu() : 1);
                if (util != null) {
                    sum += util;
                    samples++;
                }
            } catch (Exception e) {
                log.debug("No Gnocchi data for VM {}: {}", vm.getExternalId(), e.getMessage());
            }
        }

        boolean dataAvailable = samples > 0;
        Double avgCpuUtil = dataAvailable ? sum / samples : null;
        return new MetricsSummary(vmCount, runningCount, totalVcpu, totalRamMb, totalStorageGb, avgCpuUtil, dataAvailable);
    }

    /**
     * Each measure is [timestamp, granularity_seconds, cumulative_ns]. Needs at least two
     * samples to derive a rate. Result is clamped to [0, 100] — a stale/idle VM can otherwise
     * report noise slightly above 100 due to multi-core scheduling jitter.
     */
    private Double cpuUtilFromCumulativeMeasures(List<List<Object>> measures, int vcpus) {
        if (measures == null || measures.size() < 2) return null;
        List<Object> prev = measures.get(measures.size() - 2);
        List<Object> last = measures.get(measures.size() - 1);
        if (!(prev.get(1) instanceof Number granularityNum) || !(prev.get(2) instanceof Number prevValue)
                || !(last.get(2) instanceof Number lastValue)) {
            return null;
        }
        double deltaNs = lastValue.doubleValue() - prevValue.doubleValue();
        double windowNs = granularityNum.doubleValue() * 1_000_000_000.0 * Math.max(1, vcpus);
        if (windowNs <= 0 || deltaNs < 0) return null;
        return Math.min(100.0, Math.max(0.0, (deltaNs / windowNs) * 100.0));
    }
}
