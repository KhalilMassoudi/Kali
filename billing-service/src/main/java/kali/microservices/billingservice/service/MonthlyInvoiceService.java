package kali.microservices.billingservice.service;

import kali.microservices.billingservice.dto.MonthlyInvoice;
import kali.microservices.billingservice.dto.MonthlyInvoiceSummary;
import kali.microservices.billingservice.entities.UsageRecord;
import kali.microservices.billingservice.entities.WalletTransaction;
import kali.microservices.billingservice.repository.UsageRecordRepository;
import kali.microservices.billingservice.repository.WalletTransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Monthly invoices, built on the fly from the wallet ledger + the metering breakdown (nothing is
 * stored, so any past or current month can be (re)generated and always matches the ledger).
 * Pay-as-you-go: the invoice documents what was consumed and credited over the month; the amounts
 * were already settled against the prepaid balance.
 */
@Service
@RequiredArgsConstructor
public class MonthlyInvoiceService {

    private static final DateTimeFormatter NUMBER_MONTH = DateTimeFormatter.ofPattern("yyyyMM");

    private final WalletService walletService;
    private final WalletTransactionRepository transactionRepository;
    private final UsageRecordRepository usageRecordRepository;

    public static String invoiceNumber(Long userId, YearMonth month) {
        return "INV-" + month.format(NUMBER_MONTH) + "-" + userId;
    }

    /** Newest first, from the month of the client's first ledger entry up to the current month. */
    public List<MonthlyInvoiceSummary> listInvoices(Long userId) {
        String currency = currency(userId);
        List<WalletTransaction> all = new ArrayList<>(transactionRepository.findByUserIdOrderByCreatedAtDesc(userId));
        all.sort(Comparator.comparing(WalletTransaction::getCreatedAt).thenComparing(WalletTransaction::getId));

        YearMonth now = YearMonth.now();
        YearMonth first = all.isEmpty() ? now : YearMonth.from(all.get(0).getCreatedAt());

        Map<YearMonth, List<WalletTransaction>> byMonth = new HashMap<>();
        for (WalletTransaction tx : all) {
            byMonth.computeIfAbsent(YearMonth.from(tx.getCreatedAt()), k -> new ArrayList<>()).add(tx);
        }

        List<MonthlyInvoiceSummary> result = new ArrayList<>();
        BigDecimal opening = BigDecimal.ZERO;
        for (YearMonth month = first; !month.isAfter(now); month = month.plusMonths(1)) {
            MonthlyInvoiceSummary summary = summarize(userId, month, opening, byMonth.getOrDefault(month, List.of()), currency);
            result.add(summary);
            opening = summary.closingBalance();
        }
        Collections.reverse(result);
        return result;
    }

    public MonthlyInvoice buildInvoice(Long userId, YearMonth month, String clientName, String clientEmail) {
        LocalDateTime from = month.atDay(1).atStartOfDay();
        LocalDateTime to = month.plusMonths(1).atDay(1).atStartOfDay();

        BigDecimal opening = transactionRepository.findFirstByUserIdAndCreatedAtLessThanOrderByCreatedAtDescIdDesc(userId, from)
                .map(WalletTransaction::getBalanceAfter)
                .orElse(BigDecimal.ZERO);
        List<WalletTransaction> txs = transactionRepository
                .findByUserIdAndCreatedAtGreaterThanEqualAndCreatedAtLessThanOrderByCreatedAtAscIdAsc(userId, from, to);
        List<UsageRecord> records = usageRecordRepository
                .findByUserIdAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(userId, from, to);

        MonthlyInvoiceSummary summary = summarize(userId, month, opening, txs, currency(userId));

        // Group the breakdown by type and unit price (a mid-month price change gives two lines).
        Map<String, BigDecimal[]> grouped = new TreeMap<>();
        Map<String, UsageRecord> groupSample = new HashMap<>();
        Set<Long> detailedTxIds = new HashSet<>();
        for (UsageRecord r : records) {
            String key = r.getType().ordinal() + "|" + r.getUnitPrice().stripTrailingZeros().toPlainString();
            BigDecimal[] acc = grouped.computeIfAbsent(key, k -> new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO});
            acc[0] = acc[0].add(r.getQuantity());
            acc[1] = acc[1].add(r.getAmount());
            groupSample.putIfAbsent(key, r);
            if (r.getTransactionId() != null) detailedTxIds.add(r.getTransactionId());
        }

        List<MonthlyInvoice.UsageLine> lines = new ArrayList<>();
        for (Map.Entry<String, BigDecimal[]> e : grouped.entrySet()) {
            UsageRecord sample = groupSample.get(e.getKey());
            lines.add(new MonthlyInvoice.UsageLine(label(sample.getType()), unit(sample.getType()),
                    e.getValue()[0], sample.getUnitPrice(), e.getValue()[1]));
        }

        // Ticks metered before the per-type breakdown existed only have their total.
        BigDecimal undetailed = BigDecimal.ZERO;
        for (WalletTransaction tx : txs) {
            if (tx.getType() == WalletTransaction.TransactionType.USAGE_DEDUCTION && !detailedTxIds.contains(tx.getId())) {
                undetailed = undetailed.add(tx.getAmount().negate());
            }
        }
        if (undetailed.signum() != 0) {
            lines.add(new MonthlyInvoice.UsageLine("Consommation (détail non disponible)", null, null, null, undetailed));
        }

        List<MonthlyInvoice.LedgerLine> credits = txs.stream()
                .filter(tx -> tx.getType() != WalletTransaction.TransactionType.USAGE_DEDUCTION)
                .map(tx -> new MonthlyInvoice.LedgerLine(tx.getCreatedAt(),
                        tx.getDescription() != null ? tx.getDescription()
                                : (tx.getType() == WalletTransaction.TransactionType.RECHARGE ? "Recharge de crédit" : "Ajustement administrateur"),
                        tx.getAmount()))
                .toList();

        return new MonthlyInvoice(summary, userId, clientName, clientEmail, lines, credits);
    }

    private MonthlyInvoiceSummary summarize(Long userId, YearMonth month, BigDecimal opening,
                                            List<WalletTransaction> txs, String currency) {
        BigDecimal usage = BigDecimal.ZERO;
        BigDecimal recharges = BigDecimal.ZERO;
        BigDecimal adjustments = BigDecimal.ZERO;
        BigDecimal closing = opening;
        for (WalletTransaction tx : txs) {
            switch (tx.getType()) {
                case USAGE_DEDUCTION -> usage = usage.add(tx.getAmount().negate());
                case RECHARGE -> recharges = recharges.add(tx.getAmount());
                case ADMIN_ADJUSTMENT -> adjustments = adjustments.add(tx.getAmount());
            }
            closing = tx.getBalanceAfter();
        }
        return new MonthlyInvoiceSummary(month.toString(), invoiceNumber(userId, month), opening, usage, recharges,
                adjustments, closing, currency, month.equals(YearMonth.now()));
    }

    private String currency(Long userId) {
        String currency = walletService.getOrCreateWallet(userId).getCurrency();
        return currency != null ? currency : "TND";
    }

    private static String label(UsageRecord.UsageType type) {
        return switch (type) {
            case VCPU -> "Calcul - vCPU";
            case RAM -> "Calcul - mémoire RAM";
            case VM_STORAGE -> "Stockage - disques des VM";
            case VOLUME -> "Stockage - volumes";
            case FLOATING_IP -> "IP flottantes";
        };
    }

    private static String unit(UsageRecord.UsageType type) {
        return switch (type) {
            case VCPU -> "vCPU·h";
            case RAM, VM_STORAGE, VOLUME -> "Go·h";
            case FLOATING_IP -> "IP·h";
        };
    }
}
