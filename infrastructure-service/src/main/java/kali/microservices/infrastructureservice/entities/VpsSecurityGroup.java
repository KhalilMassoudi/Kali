package kali.microservices.infrastructureservice.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * A client's own security group. Rules are not persisted here — OpenStack is the source of
 * truth for rules, read live (same reasoning as the existing VM-assigned-groups read).
 */
@Entity
@Table(name = "vps_security_groups")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class VpsSecurityGroup {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private String name;

    private String description;

    @Column(unique = true)
    private String externalId; // Neutron security-group UUID

    // Optional app-level grouping (ClientProject.id). Null = ungrouped. Never an OpenStack concept.
    private Long projectId;

    @Enumerated(EnumType.STRING)
    private SecurityGroupStatus status = SecurityGroupStatus.ACTIVE;

    @CreationTimestamp
    private LocalDateTime createdAt;

    public enum SecurityGroupStatus {
        ACTIVE, DELETED, ERROR
    }
}
