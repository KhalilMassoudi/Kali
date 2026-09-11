package kali.microservices.infrastructureservice.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * A Cinder block-level snapshot of a client's volume. Distinct from the VM-backup feature
 * (a Nova server snapshotted into a Glance image, which boots a new instance) — this restores
 * or clones a single disk, not a bootable server.
 */
@Entity
@Table(name = "vps_volume_snapshots")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class VpsVolumeSnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private Long sourceVolumeId; // FK-by-convention to VpsVolume.id

    @Column(nullable = false)
    private String name;

    private String description;

    @Column(unique = true)
    private String externalId; // Cinder snapshot UUID

    private Integer sizeGb;

    @Enumerated(EnumType.STRING)
    private SnapshotStatus status = SnapshotStatus.CREATING;

    @CreationTimestamp
    private LocalDateTime createdAt;

    public enum SnapshotStatus {
        CREATING, AVAILABLE, ERROR, DELETED
    }
}
