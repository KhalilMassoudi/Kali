package kali.microservices.infrastructureservice.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * A client's Neutron router. Interfaces are not persisted here — read live from OpenStack
 * (ports whose device_id is this router's externalId), same "OpenStack is the source of
 * truth" pattern already used for security-group rules.
 */
@Entity
@Table(name = "client_routers")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ClientRouter {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private String name;

    @Column(unique = true, nullable = false)
    private String externalId; // Neutron router UUID

    private String externalGatewayNetworkId; // nullable — the provider network this router is gated to

    @CreationTimestamp
    private LocalDateTime createdAt;
}
