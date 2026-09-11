package kali.microservices.infrastructureservice.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * App-level ownership tracking for a floating IP — OpenStack itself has no per-tenant
 * isolation here (shared service-account project, same reasoning as VpsNetwork), so
 * ownership and VM association are tracked entirely in this row.
 */
@Entity
@Table(name = "client_floating_ips")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ClientFloatingIp {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(unique = true, nullable = false)
    private String floatingIpAddress;

    private String pool;

    private Long associatedVpsId; // nullable FK-by-convention to VpsServer.id

    @CreationTimestamp
    private LocalDateTime createdAt;
}
