package kali.microservices.billingservice.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Per-resource-type breakdown of one metering tick's USAGE_DEDUCTION ledger entry - the wallet
 * ledger only stores the tick's total, this keeps the quantities (unit-hours) and the unit price
 * applied so monthly invoices can show proper line items. The amounts of a tick's records always
 * add up exactly to the linked transaction's amount.
 */
@Entity
@Table(name = "usage_records", indexes = @Index(name = "idx_usage_records_user_created", columnList = "userId, createdAt"))
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UsageRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    // The USAGE_DEDUCTION WalletTransaction this record breaks down.
    private Long transactionId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UsageType type;

    // In the type's unit-hours: vCPU·h, GB RAM·h, GB·h of storage, or IP·h.
    @Column(nullable = false, precision = 18, scale = 6)
    private BigDecimal quantity;

    @Column(nullable = false, precision = 10, scale = 6)
    private BigDecimal unitPrice;

    @Column(nullable = false, precision = 12, scale = 4)
    private BigDecimal amount;

    @CreationTimestamp
    private LocalDateTime createdAt;

    public enum UsageType {
        VCPU, RAM, VM_STORAGE, VOLUME, FLOATING_IP
    }
}
