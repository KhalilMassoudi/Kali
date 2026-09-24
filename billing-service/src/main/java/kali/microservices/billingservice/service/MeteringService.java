package kali.microservices.billingservice.service;

import kali.microservices.billingservice.client.FloatingIpSnapshot;
import kali.microservices.billingservice.client.InfrastructureClient;
import kali.microservices.billingservice.client.VmSnapshot;
import kali.microservices.billingservice.client.VolumeSnapshot;
import kali.microservices.billingservice.entities.PricingConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * AWS-style metering tick: compute (vCPU+RAM) only bills while a VM is RUNNING; storage
 * (VM root disk + attached volumes) and floating IPs bill regardless of VM power state,
 * as long as the resource itself hasn't been deleted. Runs on a fixed interval rather
 * than reacting to individual events - this codebase has no message bus, and a periodic
 * sweep of infrastructure-service's fleet-wide admin views is the consistent choice given
 * every other cross-service call here is a direct synchronous HTTP request too.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MeteringService {

    private final InfrastructureClient infrastructureClient;
    private final PricingService pricingService;
    private final WalletService walletService;

    @Value("${billing.metering.interval-minutes}")
    private int intervalMinutes;

    @Scheduled(fixedRateString = "${billing.metering.interval-minutes}", timeUnit = java.util.concurrent.TimeUnit.MINUTES)
    public void meterUsage() {
        BigDecimal tickHours = BigDecimal.valueOf(intervalMinutes).divide(BigDecimal.valueOf(60), 8, RoundingMode.HALF_UP);
        PricingConfig pricing = pricingService.getConfig();

        List<VmSnapshot> vms = infrastructureClient.getAllVms();
        List<VolumeSnapshot> volumes = infrastructureClient.getAllVolumes();
        List<FloatingIpSnapshot> floatingIps = infrastructureClient.getAllFloatingIps();

        Map<Long, BigDecimal> costByUser = new HashMap<>();
        Map<Long, int[]> countsByUser = new HashMap<>(); // [runningVms, billedVms, volumes, floatingIps]

        for (VmSnapshot vm : vms) {
            if (vm.userId() == null) continue;
            int[] counts = countsByUser.computeIfAbsent(vm.userId(), k -> new int[4]);
            BigDecimal cost = BigDecimal.ZERO;

            if (vm.isRunning()) {
                BigDecimal cpuCost = pricing.getPricePerVcpuHour().multiply(BigDecimal.valueOf(vm.cpu() != null ? vm.cpu() : 0));
                BigDecimal ramGb = BigDecimal.valueOf(vm.ram() != null ? vm.ram() : 0).divide(BigDecimal.valueOf(1024), 8, RoundingMode.HALF_UP);
                BigDecimal ramCost = pricing.getPricePerRamGbHour().multiply(ramGb);
                cost = cost.add(cpuCost).add(ramCost);
                counts[0]++;
            }
            if (vm.isBillableForStorage()) {
                BigDecimal storageCost = pricing.getPricePerStorageGbHour().multiply(BigDecimal.valueOf(vm.storage() != null ? vm.storage() : 0));
                cost = cost.add(storageCost);
                counts[1]++;
            }

            cost = cost.multiply(tickHours);
            costByUser.merge(vm.userId(), cost, BigDecimal::add);
        }

        for (VolumeSnapshot vol : volumes) {
            if (vol.userId() == null || !vol.isBillable()) continue;
            int[] counts = countsByUser.computeIfAbsent(vol.userId(), k -> new int[4]);
            BigDecimal cost = pricing.getPricePerStorageGbHour()
                    .multiply(BigDecimal.valueOf(vol.sizeGb() != null ? vol.sizeGb() : 0))
                    .multiply(tickHours);
            costByUser.merge(vol.userId(), cost, BigDecimal::add);
            counts[2]++;
        }

        for (FloatingIpSnapshot ip : floatingIps) {
            if (ip.userId() == null) continue;
            int[] counts = countsByUser.computeIfAbsent(ip.userId(), k -> new int[4]);
            BigDecimal cost = pricing.getPricePerFloatingIpHour().multiply(tickHours);
            costByUser.merge(ip.userId(), cost, BigDecimal::add);
            counts[3]++;
        }

        int billedUsers = 0;
        for (Map.Entry<Long, BigDecimal> entry : costByUser.entrySet()) {
            Long userId = entry.getKey();
            BigDecimal total = entry.getValue().setScale(4, RoundingMode.HALF_UP);
            if (total.compareTo(BigDecimal.ZERO) <= 0) continue;

            int[] counts = countsByUser.getOrDefault(userId, new int[4]);
            String description = String.format(
                    "Consommation (%d min): %d VM active(s), %d VM stockée(s), %d volume(s), %d IP flottante(s)",
                    intervalMinutes, counts[0], counts[1], counts[2], counts[3]);
            walletService.deductForUsage(userId, total, description, "USAGE_TICK", null);
            billedUsers++;
        }

        log.info("Metering tick complete: {} user(s) billed, interval={} min", billedUsers, intervalMinutes);
    }
}
