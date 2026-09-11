package kali.microservices.infrastructureservice.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "vps_volumes")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class VpsVolume {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private Integer sizeGb;

    @Column(unique = true)
    private String externalId;  // OpenStack Cinder volume UUID

    private Long attachedVpsId; // nullable FK to VpsServer.id
    private String device;      // e.g. /dev/vdb, set once attached
    private String region;

    // Optional app-level grouping (ClientProject.id). Null = ungrouped. Never an OpenStack concept.
    private Long projectId;

    @Enumerated(EnumType.STRING)
    private VolumeStatus status = VolumeStatus.CREATING;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    public enum VolumeStatus {
        CREATING, AVAILABLE, IN_USE, ERROR, DELETED
    }
}
