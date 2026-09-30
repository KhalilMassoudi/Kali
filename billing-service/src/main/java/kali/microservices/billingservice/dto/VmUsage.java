package kali.microservices.billingservice.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * One VM's charges (vCPU, RAM, disk), tick by tick, latest first. walletBefore/walletAfter are
 * the wallet balance around that tick's deduction - which may also include the client's other
 * resources billed in the same tick.
 */
public record VmUsage(
        Long vmId,
        String vmName,
        String currency,
        BigDecimal total,
        BigDecimal hourlyRate,
        LocalDateTime firstChargeAt,
        LocalDateTime lastChargeAt,
        List<Entry> entries
) {
    public record Entry(LocalDateTime from, LocalDateTime to, BigDecimal amount, Map<String, BigDecimal> byType,
                        BigDecimal walletBefore, BigDecimal walletAfter) {
    }
}
