package kali.microservices.infrastructureservice.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "vps_servers")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class VpsServer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String os;        // ubuntu, debian, centos...

    private Integer ram;      // en MB
    private Integer cpu;      // nombre de vCPU
    private Integer storage;  // en GB

    @Column(unique = true)
    private String externalId;  // OpenStack server UUID

    private String ipAddress;
    private String floatingIp;
    private String region;

    // Optional app-level grouping (ClientProject.id). Null = ungrouped. Never an OpenStack concept.
    private Long projectId;

    @Enumerated(EnumType.STRING)
    private VpsStatus status = VpsStatus.PENDING;

    // Nova's "locked" is a separate server attribute, not a vm_state/task_state — not folded
    // into VpsStatus.
    @Column(nullable = false)
    @ColumnDefault("false")
    private boolean locked = false;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    public enum VpsStatus {
        PENDING, RUNNING, STOPPED, DELETED, ERROR, RESIZING, VERIFY_RESIZE, RESCUE,
        PAUSED, SUSPENDED, SHELVED, SHELVED_OFFLOADED
    }
}