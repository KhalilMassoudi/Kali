package kali.microservices.billingservice.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "invoices")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Invoice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false, unique = true)
    private String invoiceNumber;   // ex: INV-2024-00001

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    private String currency = "TND";

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    private InvoiceStatus status = InvoiceStatus.PENDING;

    private LocalDate dueDate;
    private LocalDate paidAt;

    // Référence à une ressource (VPS, domaine, cluster)
    private String resourceType;   // VPS, DOMAIN, K8S_CLUSTER
    private Long resourceId;

    @CreationTimestamp
    private LocalDateTime createdAt;

    public enum InvoiceStatus {
        PENDING, PAID, OVERDUE, CANCELLED
    }
}