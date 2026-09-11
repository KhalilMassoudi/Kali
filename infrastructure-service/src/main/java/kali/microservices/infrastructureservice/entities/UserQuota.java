package kali.microservices.infrastructureservice.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * Per-user resource limits. A row is created lazily with default values the first time
 * it's needed (see QuotaService.getOrCreateDefault) so every existing user gets sane
 * defaults without a migration/backfill step.
 */
@Entity
@Table(name = "user_quotas")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserQuota {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private Long userId;

    private Integer maxVcpu = 8;
    private Integer maxRamMb = 16384;
    private Integer maxStorageGb = 200;
    private Integer maxVms = 5;
    private Integer maxVolumes = 5;
    private Integer maxNetworks = 3;
    private Integer maxSecurityGroups = 5;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
