package kali.microservices.billingservice.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "wallets")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Wallet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private Long userId;

    @Column(nullable = false, precision = 12, scale = 4)
    private BigDecimal balance = BigDecimal.ZERO;

    private String currency = "TND";

    // One-shot alert flags (nullable: added after the table existed). Set once the matching email
    // went out, cleared by WalletService when a credit brings the balance back above the level.
    private Boolean lowBalanceAlertSent;
    private Boolean exhaustedAlertSent;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
