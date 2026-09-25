package kali.microservices.billingservice.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/** Everything the PDF renderer needs for one client-month. */
public record MonthlyInvoice(
        MonthlyInvoiceSummary summary,
        Long userId,
        String clientName,
        String clientEmail,
        List<UsageLine> usageLines,
        List<LedgerLine> credits    // recharges + admin adjustments, chronological
) {
    /** quantity/unitPrice are null for legacy ticks recorded before the per-type breakdown existed. */
    public record UsageLine(String label, String unit, BigDecimal quantity, BigDecimal unitPrice, BigDecimal amount) {
    }

    public record LedgerLine(LocalDateTime date, String label, BigDecimal amount) {
    }
}
