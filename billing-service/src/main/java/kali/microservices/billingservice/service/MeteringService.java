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
import java.util.EnumMap;
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

        // Per user, per resource type: [quantity in unit-hours, amount] - becomes the tick's UsageRecords.
        Map<Long, Map<UsageRecord.UsageType, BigDecimal[]>> usageByUser = new HashMap<>();
        Map<Long, int[]> countsByUser = new HashMap<>(); // [runningVms, billedVms, volumes, floatingIps]
        Map<Long, List<VmSnapshot>> vmsByUser = new HashMap<>();

        for (VmSnapshot vm : vms) {
            if (vm.userId() == null) continue;
            vmsByUser.computeIfAbsent(vm.userId(), k -> new ArrayList<>()).add(vm);
            int[] counts = countsByUser.computeIfAbsent(vm.userId(), k -> new int[4]);

            if (vm.isRunning()) {
                BigDecimal vcpus = BigDecimal.valueOf(vm.cpu() != null ? vm.cpu() : 0);
                BigDecimal ramGb = BigDecimal.valueOf(vm.ram() != null ? vm.ram() : 0).divide(BigDecimal.valueOf(1024), 8, RoundingMode.HALF_UP);
                addUsage(usageByUser, vm.userId(), UsageRecord.UsageType.VCPU, vcpus.multiply(tickHours), pricing.getPricePerVcpuHour());
                addUsage(usageByUser, vm.userId(), UsageRecord.UsageType.RAM, ramGb.multiply(tickHours), pricing.getPricePerRamGbHour());
                counts[0]++;
            }
            if (vm.isBillableForStorage()) {
                BigDecimal storageGb = BigDecimal.valueOf(vm.storage() != null ? vm.storage() : 0);
                addUsage(usageByUser, vm.userId(), UsageRecord.UsageType.VM_STORAGE, storageGb.multiply(tickHours), pricing.getPricePerStorageGbHour());
                counts[1]++;
            }
        }

        for (VolumeSnapshot vol : volumes) {
            if (vol.userId() == null || !vol.isBillable()) continue;
            int[] counts = countsByUser.computeIfAbsent(vol.userId(), k -> new int[4]);
            BigDecimal sizeGb = BigDecimal.valueOf(vol.sizeGb() != null ? vol.sizeGb() : 0);
            addUsage(usageByUser, vol.userId(), UsageRecord.UsageType.VOLUME, sizeGb.multiply(tickHours), pricing.getPricePerStorageGbHour());
            counts[2]++;
        }

        for (FloatingIpSnapshot ip : floatingIps) {
            if (ip.userId() == null) continue;
            int[] counts = countsByUser.computeIfAbsent(ip.userId(), k -> new int[4]);
            addUsage(usageByUser, ip.userId(), UsageRecord.UsageType.FLOATING_IP, tickHours, pricing.getPricePerFloatingIpHour());
            counts[3]++;
        }

        int billedUsers = 0;
        for (Map.Entry<Long, Map<UsageRecord.UsageType, BigDecimal[]>> entry : usageByUser.entrySet()) {
            Long userId = entry.getKey();

            // Each type's amount is rounded on its own and the ledger entry is their sum - so an
            // invoice's line items always add up exactly to what was deducted.
            List<UsageRecord> breakdown = new ArrayList<>();
            BigDecimal total = BigDecimal.ZERO;
            for (Map.Entry<UsageRecord.UsageType, BigDecimal[]> usage : entry.getValue().entrySet()) {
                BigDecimal amount = usage.getValue()[1].setScale(4, RoundingMode.HALF_UP);
                if (amount.signum() <= 0) continue;
                UsageRecord record = new UsageRecord();
                record.setType(usage.getKey());
                record.setQuantity(usage.getValue()[0].setScale(6, RoundingMode.HALF_UP));
                record.setUnitPrice(unitPrice(usage.getKey(), pricing));
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

    private static void addUsage(Map<Long, Map<UsageRecord.UsageType, BigDecimal[]>> usageByUser, Long userId,
                                 UsageRecord.UsageType type, BigDecimal quantity, BigDecimal unitPrice) {
        BigDecimal[] acc = usageByUser.computeIfAbsent(userId, k -> new EnumMap<>(UsageRecord.UsageType.class))
                .computeIfAbsent(type, k -> new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO});
        acc[0] = acc[0].add(quantity);
        acc[1] = acc[1].add(quantity.multiply(unitPrice));
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
