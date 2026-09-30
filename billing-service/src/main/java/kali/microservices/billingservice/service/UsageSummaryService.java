package kali.microservices.billingservice.service;

import kali.microservices.billingservice.dto.UsageSummary;
import kali.microservices.billingservice.dto.VmUsage;
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

    private static final List<UsageRecord.UsageType> VM_TYPES =
            List.of(UsageRecord.UsageType.VCPU, UsageRecord.UsageType.RAM, UsageRecord.UsageType.VM_STORAGE);
    private static final int MAX_VM_ENTRIES = 300;

    /** Tick-by-tick charges of one VM (records metered before per-resource tracking aren't included). */
    public VmUsage vmUsage(Long userId, Long vmId) {
        List<UsageRecord> records = usageRecordRepository.findByUserIdAndResourceIdAndTypeInOrderByCreatedAtAsc(userId, vmId, VM_TYPES);
        String currency = pricingService.getConfig().getCurrency() != null ? pricingService.getConfig().getCurrency() : "TND";

        // One entry per tick (= per ledger transaction), oldest first while building.
        Map<Long, List<UsageRecord>> byTick = new LinkedHashMap<>();
        for (UsageRecord r : records) byTick.computeIfAbsent(r.getTransactionId(), k -> new ArrayList<>()).add(r);
        Map<Long, WalletTransaction> txs = transactionRepository.findAllById(
                        byTick.keySet().stream().filter(Objects::nonNull).toList())
                .stream().collect(Collectors.toMap(WalletTransaction::getId, t -> t));

        List<VmUsage.Entry> entries = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;
        String name = null;
        for (var tick : byTick.entrySet()) {
            Map<String, BigDecimal> byType = new LinkedHashMap<>();
            BigDecimal amount = BigDecimal.ZERO;
            LocalDateTime at = null;
            for (UsageRecord r : tick.getValue()) {
                byType.merge(r.getType().name(), r.getAmount(), BigDecimal::add);
                amount = amount.add(r.getAmount());
                at = r.getCreatedAt();
                if (r.getResourceName() != null) name = r.getResourceName();
            }
            WalletTransaction tx = txs.get(tick.getKey());
            BigDecimal after = tx != null ? tx.getBalanceAfter() : null;
            BigDecimal before = tx != null ? tx.getBalanceAfter().subtract(tx.getAmount()) : null;
            entries.add(new VmUsage.Entry(at != null ? at.minusMinutes(intervalMinutes) : null, at, scale(amount),
                    scaleAll(byType), before, after));
            total = total.add(amount);
        }

        BigDecimal hourlyRate = BigDecimal.ZERO;
        if (!entries.isEmpty()) {
            VmUsage.Entry last = entries.get(entries.size() - 1);
            if (last.to() != null && last.to().isAfter(LocalDateTime.now().minusMinutes(2L * intervalMinutes))) {
                hourlyRate = last.amount().multiply(BigDecimal.valueOf(60))
                        .divide(BigDecimal.valueOf(intervalMinutes), 6, RoundingMode.HALF_UP);
            }
        }
        Collections.reverse(entries);
        return new VmUsage(vmId, name, currency, scale(total), scale(hourlyRate),
                entries.isEmpty() ? null : entries.get(entries.size() - 1).from(),
                entries.isEmpty() ? null : entries.get(0).to(),
                entries.size() > MAX_VM_ENTRIES ? entries.subList(0, MAX_VM_ENTRIES) : entries);
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
