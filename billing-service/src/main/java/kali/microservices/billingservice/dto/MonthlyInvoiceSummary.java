package kali.microservices.billingservice.dto;

import java.math.BigDecimal;

/** One row of a client's "Factures" list - a month's totals, computed on the fly from the ledger. */
public record MonthlyInvoiceSummary(
        String month,             // yyyy-MM
        String invoiceNumber,     // INV-YYYYMM-<userId>
        BigDecimal openingBalance,
        BigDecimal usageTotal,    // positive: what was consumed
        BigDecimal rechargeTotal,
        BigDecimal adjustmentTotal,
        BigDecimal closingBalance,
        String currency,
        boolean current           // month still in progress - provisional invoice
) {
}
