package kali.microservices.billingservice.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * What a client consumed over a window and what it cost - backs the client "Consommation"
 * page. Amounts are positive (cost), keyed by UsageRecord.UsageType name, plus "OTHER" for
 * deductions metered before per-resource records existed.
 *
 * hourlyRate is the cost of the most recent metering tick scaled to one hour (0 when nothing
 * has been metered recently, i.e. nothing is running or allocated); runwayHours is null when
 * the rate is 0 (the balance isn't being drawn down).
 */
public record UsageSummary(
        LocalDate from,
        LocalDate to,
        String currency,
        BigDecimal total,
        BigDecimal averagePerDay,
        BigDecimal hourlyRate,
        BigDecimal projectedMonth,
        BigDecimal balance,
        BigDecimal runwayHours,
        Map<String, BigDecimal> byType,
        List<DailyUsage> daily
) {
    public record DailyUsage(LocalDate date, BigDecimal total, Map<String, BigDecimal> byType) {
    }
}
