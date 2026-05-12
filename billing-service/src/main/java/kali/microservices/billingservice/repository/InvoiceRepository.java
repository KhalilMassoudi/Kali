package kali.microservices.billingservice.repository;

import kali.microservices.billingservice.entities.Invoice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InvoiceRepository extends JpaRepository<Invoice, Long> {
    List<Invoice> findByUserIdOrderByCreatedAtDesc(Long userId);
    List<Invoice> findByUserIdAndStatus(Long userId, Invoice.InvoiceStatus status);
    Optional<Invoice> findByInvoiceNumber(String invoiceNumber);
}