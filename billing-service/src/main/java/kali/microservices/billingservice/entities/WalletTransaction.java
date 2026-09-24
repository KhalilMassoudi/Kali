package kali.microservices.billingservice.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "wallet_transactions")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class WalletTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransactionType type;

    // Positive for recharges/adjustment-credits, negative for usage deductions/adjustment-debits.
    @Column(nullable = false, precision = 12, scale = 4)
    private BigDecimal amount;

    @Column(nullable = false, precision = 12, scale = 4)
    private BigDecimal balanceAfter;

    private String description;

    // Only set for USAGE_DEDUCTION entries - what was metered.
    private String resourceType; // VM_COMPUTE, VM_STORAGE, VOLUME, FLOATING_IP
    private Long resourceId;

    @CreationTimestamp
    private LocalDateTime createdAt;

    public enum TransactionType {
        RECHARGE, USAGE_DEDUCTION, ADMIN_ADJUSTMENT
    }
}
