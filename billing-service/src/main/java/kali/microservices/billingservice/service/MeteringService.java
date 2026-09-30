package kali.microservices.billingservice.service;

import kali.microservices.billingservice.client.FloatingIpSnapshot;
import kali.microservices.billingservice.client.InfrastructureClient;
import kali.microservices.billingservice.client.VmSnapshot;
import kali.microservices.billingservice.client.VolumeSnapshot;
import kali.microservices.billingservice.entities.PricingConfig;
import kali.microservices.billingservice.entities.UsageRecord;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
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
    private final BalanceAlertService balanceAlertService;

    @Value("${billing.metering.interval-minutes}")
    private int intervalMinutes;

    @Scheduled(fixedRateString = "${billing.metering.interval-minutes}", timeUnit = java.util.concurrent.TimeUnit.MINUTES)
    public void meterUsage() {
        BigDecimal tickHours = BigDecimal.valueOf(intervalMinutes).divide(BigDecimal.valueOf(60), 8, RoundingMode.HALF_UP);
        PricingConfig pricing = pricingService.getConfig();

        List<VmSnapshot> vms = infrastructureClient.getAllVms();
        List<VolumeSnapshot> volumes = infrastructureClient.getAllVolumes();
        List<FloatingIpSnapshot> floatingIps = infrastructureClient.getAllFloatingIps();

        // Per user, one line per (resource, type): quantity in unit-hours and amount - becomes the tick's UsageRecords.
        Map<Long, Map<String, Line>> usageByUser = new HashMap<>();
        Map<Long, int[]> countsByUser = new HashMap<>(); // [runningVms, billedVms, volumes, floatingIps]
        Map<Long, List<VmSnapshot>> vmsByUser = new HashMap<>();

        for (VmSnapshot vm : vms) {
            if (vm.userId() == null) continue;
            vmsByUser.computeIfAbsent(vm.userId(), k -> new ArrayList<>()).add(vm);
            int[] counts = countsByUser.computeIfAbsent(vm.userId(), k -> new int[4]);

            if (vm.isRunning()) {
                BigDecimal vcpus = BigDecimal.valueOf(vm.cpu() != null ? vm.cpu() : 0);
                BigDecimal ramGb = BigDecimal.valueOf(vm.ram() != null ? vm.ram() : 0).divide(BigDecimal.valueOf(1024), 8, RoundingMode.HALF_UP);
                addUsage(usageByUser, vm.userId(), UsageRecord.UsageType.VCPU, vm.id(), vm.name(), vcpus.multiply(tickHours), pricing.getPricePerVcpuHour());
                addUsage(usageByUser, vm.userId(), UsageRecord.UsageType.RAM, vm.id(), vm.name(), ramGb.multiply(tickHours), pricing.getPricePerRamGbHour());
                counts[0]++;
            }
            if (vm.isBillableForStorage()) {
                BigDecimal storageGb = BigDecimal.valueOf(vm.storage() != null ? vm.storage() : 0);
                addUsage(usageByUser, vm.userId(), UsageRecord.UsageType.VM_STORAGE, vm.id(), vm.name(), storageGb.multiply(tickHours), pricing.getPricePerStorageGbHour());
                counts[1]++;
            }
        }

        for (VolumeSnapshot vol : volumes) {
            if (vol.userId() == null || !vol.isBillable()) continue;
            int[] counts = countsByUser.computeIfAbsent(vol.userId(), k -> new int[4]);
            BigDecimal sizeGb = BigDecimal.valueOf(vol.sizeGb() != null ? vol.sizeGb() : 0);
            addUsage(usageByUser, vol.userId(), UsageRecord.UsageType.VOLUME, vol.id(), null, sizeGb.multiply(tickHours), pricing.getPricePerStorageGbHour());
            counts[2]++;
        }

        for (FloatingIpSnapshot ip : floatingIps) {
            if (ip.userId() == null) continue;
            int[] counts = countsByUser.computeIfAbsent(ip.userId(), k -> new int[4]);
            addUsage(usageByUser, ip.userId(), UsageRecord.UsageType.FLOATING_IP, ip.id(), ip.floatingIpAddress(), tickHours, pricing.getPricePerFloatingIpHour());
            counts[3]++;
        }

        int billedUsers = 0;
        for (Map.Entry<Long, Map<String, Line>> entry : usageByUser.entrySet()) {
            Long userId = entry.getKey();

            // Each line's amount is rounded on its own and the ledger entry is their sum - so an
            // invoice's line items (and a VM's history) always add up exactly to what was deducted.
            List<UsageRecord> breakdown = new ArrayList<>();
            BigDecimal total = BigDecimal.ZERO;
            for (Line line : entry.getValue().values()) {
                BigDecimal amount = line.amount.setScale(4, RoundingMode.HALF_UP);
                if (amount.signum() <= 0) continue;
                UsageRecord record = new UsageRecord();
                record.setType(line.type);
                record.setResourceId(line.resourceId);
                record.setResourceName(line.resourceName);
                record.setQuantity(line.quantity.setScale(6, RoundingMode.HALF_UP));
                record.setUnitPrice(unitPrice(line.type, pricing));
                record.setAmount(amount);
                breakdown.add(record);
                total = total.add(amount);
            }
            if (total.compareTo(BigDecimal.ZERO) <= 0) continue;

            int[] counts = countsByUser.getOrDefault(userId, new int[4]);
            String description = String.format(
                    "Consommation (%d min): %d VM active(s), %d VM stockée(s), %d volume(s), %d IP flottante(s)",
                    intervalMinutes, counts[0], counts[1], counts[2], counts[3]);
            walletService.deductForUsage(userId, total, description, "USAGE_TICK", null, breakdown);
            billedUsers++;

            try {
                balanceAlertService.evaluate(userId, vmsByUser.getOrDefault(userId, List.of()));
            } catch (Exception e) {
                log.error("Balance alert check failed for userId={}: {}", userId, e.getMessage());
            }
        }

        log.info("Metering tick complete: {} user(s) billed, interval={} min", billedUsers, intervalMinutes);
    }

    /** One billed line of a tick: a resource and a usage type. */
    private static final class Line {
        final UsageRecord.UsageType type;
        final Long resourceId;
        final String resourceName;
        BigDecimal quantity = BigDecimal.ZERO;
        BigDecimal amount = BigDecimal.ZERO;

        Line(UsageRecord.UsageType type, Long resourceId, String resourceName) {
            this.type = type;
            this.resourceId = resourceId;
            this.resourceName = resourceName;
        }
    }

    private static void addUsage(Map<Long, Map<String, Line>> usageByUser, Long userId, UsageRecord.UsageType type,
                                 Long resourceId, String resourceName, BigDecimal quantity, BigDecimal unitPrice) {
        Line line = usageByUser.computeIfAbsent(userId, k -> new LinkedHashMap<>())
                .computeIfAbsent(type + ":" + resourceId, k -> new Line(type, resourceId, resourceName));
        line.quantity = line.quantity.add(quantity);
        line.amount = line.amount.add(quantity.multiply(unitPrice));
    }

    private static BigDecimal unitPrice(UsageRecord.UsageType type, PricingConfig pricing) {
        return switch (type) {
            case VCPU -> pricing.getPricePerVcpuHour();
            case RAM -> pricing.getPricePerRamGbHour();
            case VM_STORAGE, VOLUME -> pricing.getPricePerStorageGbHour();
            case FLOATING_IP -> pricing.getPricePerFloatingIpHour();
        };
    }
}
