package kali.microservices.billingservice.service;

import kali.microservices.billingservice.dto.MonthlyInvoice;
import kali.microservices.billingservice.dto.MonthlyInvoiceSummary;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InvoicePdfRendererTest {

    @Test
    void rendersAPdf() {
        MonthlyInvoiceSummary summary = new MonthlyInvoiceSummary("2026-08", MonthlyInvoiceService.invoiceNumber(42L, java.time.YearMonth.of(2026, 8)),
                new BigDecimal("12.5000"), new BigDecimal("9.3100"), new BigDecimal("20.0000"), new BigDecimal("-1.0000"),
                new BigDecimal("22.1900"), "TND", false);
        MonthlyInvoice invoice = new MonthlyInvoice(summary, 42L, "Amira Ben Salah", "amira@example.com",
                List.of(
                        new MonthlyInvoice.UsageLine("Calcul - vCPU", "vCPU·h", new BigDecimal("120.5"), new BigDecimal("0.050000"), new BigDecimal("6.0250")),
                        new MonthlyInvoice.UsageLine("IP flottantes", "IP·h", new BigDecimal("744"), new BigDecimal("0.005000"), new BigDecimal("3.7200")),
                        new MonthlyInvoice.UsageLine("Consommation (détail non disponible)", null, null, null, new BigDecimal("0.4350"))),
                List.of(new MonthlyInvoice.LedgerLine(LocalDateTime.of(2026, 8, 3, 10, 0), "Recharge de crédit", new BigDecimal("20.0000")),
                        new MonthlyInvoice.LedgerLine(LocalDateTime.of(2026, 8, 9, 10, 0), "Ajustement manuel (admin)", new BigDecimal("-1.0000"))));

        byte[] pdf = new InvoicePdfRenderer().render(invoice);

        assertTrue(pdf.length > 1000);
        assertEquals("%PDF", new String(pdf, 0, 4, StandardCharsets.US_ASCII));
    }

    @Test
    void invoiceNumberIsDeterministic() {
        assertEquals("INV-202609-7", MonthlyInvoiceService.invoiceNumber(7L, java.time.YearMonth.of(2026, 9)));
    }
}
