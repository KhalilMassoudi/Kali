package kali.microservices.billingservice.service;

import kali.microservices.billingservice.dto.UsageSummary;
import kali.microservices.billingservice.entities.UsageRecord;
import kali.microservices.billingservice.entities.WalletTransaction;
import kali.microservices.billingservice.repository.UsageRecordRepository;
import kali.microservices.billingservice.repository.WalletTransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Aggregates a client's metering history (UsageRecords, one per resource type per tick) into
 * per-day / per-type spend for the "Consommation" page.
 */
@Service
@RequiredArgsConstructor
public class UsageSummaryService {

    private static final String OTHER = "OTHER";

    private final UsageRecordRepository usageRecordRepository;
    private final WalletTransactionRepository transactionRepository;
    private final WalletService walletService;
    private final PricingService pricingService;

    @Value("${billing.metering.interval-minutes:10}")
    private int intervalMinutes;

    public UsageSummary summarize(Long userId, int days) {
        LocalDate to = LocalDate.now();
        LocalDate from = to.minusDays(days - 1L);
        LocalDateTime start = from.atStartOfDay();
        LocalDateTime end = LocalDateTime.now().plusSeconds(1);

        List<UsageRecord> records = usageRecordRepository
                .findByUserIdAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(userId, start, end);
        List<WalletTransaction> deductions = transactionRepository
                .findByUserIdAndCreatedAtGreaterThanEqualAndCreatedAtLessThanOrderByCreatedAtAscIdAsc(userId, start, end)
                .stream().filter(t -> t.getType() == WalletTransaction.TransactionType.USAGE_DEDUCTION)
                .toList();

        // day -> type -> amount
        Map<LocalDate, Map<String, BigDecimal>> perDay = new TreeMap<>();
        for (LocalDate d = from; !d.isAfter(to); d = d.plusDays(1)) perDay.put(d, new LinkedHashMap<>());
        for (UsageRecord r : records) {
            add(perDay, r.getCreatedAt().toLocalDate(), r.getType().name(), r.getAmount());
        }
        // Deductions metered before UsageRecords existed have no breakdown - keep them as "OTHER"
        // so totals still match what was actually debited.
        Set<Long> itemized = records.stream().map(UsageRecord::getTransactionId).filter(Objects::nonNull).collect(Collectors.toSet());
        for (WalletTransaction t : deductions) {
            if (!itemized.contains(t.getId())) add(perDay, t.getCreatedAt().toLocalDate(), OTHER, t.getAmount().abs());
        }

        Map<String, BigDecimal> byType = new LinkedHashMap<>();
        List<UsageSummary.DailyUsage> daily = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;
        for (var e : perDay.entrySet()) {
            BigDecimal dayTotal = e.getValue().values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
            e.getValue().forEach((type, amount) -> byType.merge(type, amount, BigDecimal::add));
            daily.add(new UsageSummary.DailyUsage(e.getKey(), scale(dayTotal), scaleAll(e.getValue())));
            total = total.add(dayTotal);
        }

        BigDecimal hourlyRate = currentHourlyRate(deductions);
        BigDecimal balance = walletService.getOrCreateWallet(userId).getBalance();
        BigDecimal runwayHours = hourlyRate.signum() > 0 && balance.signum() > 0
                ? balance.divide(hourlyRate, 1, RoundingMode.DOWN) : null;
        String currency = pricingService.getConfig().getCurrency() != null ? pricingService.getConfig().getCurrency() : "TND";

        return new UsageSummary(from, to, currency, scale(total),
                scale(total.divide(BigDecimal.valueOf(days), 6, RoundingMode.HALF_UP)),
                scale(hourlyRate), scale(hourlyRate.multiply(BigDecimal.valueOf(24L * 30))),
                balance, runwayHours, scaleAll(byType), daily);
    }

    /**
     * Last tick's cost scaled to one hour. A tick older than two intervals means nothing has
     * been metered since (all resources stopped/deleted), so the burn rate is 0.
     */
    private BigDecimal currentHourlyRate(List<WalletTransaction> deductions) {
        if (deductions.isEmpty()) return BigDecimal.ZERO;
        WalletTransaction last = deductions.get(deductions.size() - 1);
        if (last.getCreatedAt().isBefore(LocalDateTime.now().minusMinutes(2L * intervalMinutes))) return BigDecimal.ZERO;
        return last.getAmount().abs()
                .multiply(BigDecimal.valueOf(60))
                .divide(BigDecimal.valueOf(intervalMinutes), 6, RoundingMode.HALF_UP);
    }

    private static void add(Map<LocalDate, Map<String, BigDecimal>> perDay, LocalDate day, String type, BigDecimal amount) {
        Map<String, BigDecimal> m = perDay.get(day);
        if (m != null && amount != null) m.merge(type, amount, BigDecimal::add);
    }

    private static BigDecimal scale(BigDecimal v) {
        return v.setScale(4, RoundingMode.HALF_UP);
    }

    private static Map<String, BigDecimal> scaleAll(Map<String, BigDecimal> m) {
        Map<String, BigDecimal> out = new LinkedHashMap<>();
        m.forEach((k, v) -> out.put(k, scale(v)));
        return out;
    }
}
