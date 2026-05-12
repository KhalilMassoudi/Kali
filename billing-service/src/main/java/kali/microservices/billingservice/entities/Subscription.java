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
@Table(name = "subscriptions")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Subscription {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    private Plan plan;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal monthlyPrice;

    private String currency = "TND";

    @Enumerated(EnumType.STRING)
    private SubscriptionStatus status = SubscriptionStatus.ACTIVE;

    private LocalDate startDate;
    private LocalDate endDate;
    private LocalDate nextBillingDate;

    @CreationTimestamp
    private LocalDateTime createdAt;

    public enum Plan {
        STARTER, PROFESSIONAL, ENTERPRISE
    }

    public enum SubscriptionStatus {
        ACTIVE, CANCELLED, EXPIRED, SUSPENDED
    }
}