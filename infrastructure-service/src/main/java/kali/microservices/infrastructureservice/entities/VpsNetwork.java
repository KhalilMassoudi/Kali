package kali.microservices.infrastructureservice.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * A client's own private network. OpenStack itself has no per-tenant isolation here (every
 * VM in this platform runs under one shared service-account project), so ownership is tracked
 * entirely in this row — mirrors VpsVolume's shape/pattern.
 */
@Entity
@Table(name = "vps_networks")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class VpsNetwork {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private String name;

    @Column(unique = true)
    private String externalId; // Neutron network UUID

    private String subnetId;   // Neutron subnet UUID
    private String cidr;

    // Optional app-level grouping (ClientProject.id). Null = ungrouped. Never an OpenStack concept.
    private Long projectId;

    @Enumerated(EnumType.STRING)
    private NetworkStatus status = NetworkStatus.CREATING;

    @CreationTimestamp
    private LocalDateTime createdAt;

    public enum NetworkStatus {
        CREATING, ACTIVE, DELETED, ERROR
    }
}
