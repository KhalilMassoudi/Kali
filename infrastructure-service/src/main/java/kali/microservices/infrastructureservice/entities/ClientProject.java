package kali.microservices.infrastructureservice.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * A client's own logical grouping of their resources (VMs, volumes, networks, security
 * groups). Purely an app-level label — every resource still runs inside the single shared
 * OpenStack project (safozi-app), exactly like today. {@code externalId} is reserved for a
 * future real Keystone-project linkage and is always null under the current model; when/if
 * per-client OpenStack projects are ever provisioned, this column becomes the join key
 * without requiring a schema change here or on any of the four resource tables.
 */
@Entity
@Table(name = "client_projects")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ClientProject {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private String name;

    private String description;

    @Column(unique = true)
    private String externalId; // reserved for a future real Keystone project id; always null today

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
