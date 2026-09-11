package kali.microservices.infrastructureservice.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * A client's Nova server group (affinity/anti-affinity scheduling hint). Membership is not
 * persisted here — Nova tracks it automatically as VMs join at boot; read live via
 * CloudProvider.getServerGroup, same "OpenStack is the source of truth" pattern already used
 * for security-group rules.
 */
@Entity
@Table(name = "client_server_groups")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ClientServerGroup {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private String name;

    @Column(unique = true, nullable = false)
    private String externalId; // Nova server group UUID

    @Enumerated(EnumType.STRING)
    private Policy policy;

    @CreationTimestamp
    private LocalDateTime createdAt;

    public enum Policy {
        AFFINITY, ANTI_AFFINITY, SOFT_AFFINITY, SOFT_ANTI_AFFINITY;

        public String toOpenStackValue() {
            return name().toLowerCase().replace('_', '-');
        }
    }
}
