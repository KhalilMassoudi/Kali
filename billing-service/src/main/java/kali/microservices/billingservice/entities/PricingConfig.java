package kali.microservices.billingservice.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Singleton config (always id=1) - admin-editable price-per-unit rates used by the
 * metering job. AWS-style: compute only bills while a VM is actually RUNNING; storage
 * (VM root disk + attached volumes) and floating IPs bill regardless of VM power state.
 */
@Entity
@Table(name = "pricing_config")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PricingConfig {

    @Id
    private Long id = 1L;

    @Column(nullable = false, precision = 10, scale = 6)
    private BigDecimal pricePerVcpuHour = new BigDecimal("0.050000");

    @Column(nullable = false, precision = 10, scale = 6)
    private BigDecimal pricePerRamGbHour = new BigDecimal("0.010000");

    @Column(nullable = false, precision = 10, scale = 6)
    private BigDecimal pricePerStorageGbHour = new BigDecimal("0.002000");

    @Column(nullable = false, precision = 10, scale = 6)
    private BigDecimal pricePerFloatingIpHour = new BigDecimal("0.005000");

    private String currency = "TND";

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
