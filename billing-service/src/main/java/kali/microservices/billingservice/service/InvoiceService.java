package kali.microservices.billingservice.service;

import kali.microservices.billingservice.entities.Invoice;
import kali.microservices.billingservice.repository.InvoiceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

@Service
@RequiredArgsConstructor
public class InvoiceService {

    private final InvoiceRepository invoiceRepository;
    private static final AtomicLong invoiceCounter = new AtomicLong(1);

    public Invoice createInvoice(Long userId, BigDecimal amount, String description,
                                  String resourceType, Long resourceId) {
        Invoice invoice = new Invoice();
        invoice.setUserId(userId);
        invoice.setInvoiceNumber(generateInvoiceNumber());
        invoice.setAmount(amount);
        invoice.setDescription(description);
        invoice.setResourceType(resourceType);
        invoice.setResourceId(resourceId);
        invoice.setDueDate(LocalDate.now().plusDays(30));
        invoice.setStatus(Invoice.InvoiceStatus.PENDING);

        return invoiceRepository.save(invoice);
    }

    public Invoice payInvoice(Long invoiceId) {
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new RuntimeException("Facture non trouvée: " + invoiceId));
        invoice.setStatus(Invoice.InvoiceStatus.PAID);
        invoice.setPaidAt(LocalDate.now());
        return invoiceRepository.save(invoice);
    }

    public List<Invoice> getInvoicesByUser(Long userId) {
        return invoiceRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    public Invoice getInvoiceById(Long id) {
        return invoiceRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Facture non trouvée: " + id));
    }

    private String generateInvoiceNumber() {
        int year = LocalDate.now().getYear();
        long count = invoiceRepository.count() + 1;
        return String.format("INV-%d-%05d", year, count);
    }
}