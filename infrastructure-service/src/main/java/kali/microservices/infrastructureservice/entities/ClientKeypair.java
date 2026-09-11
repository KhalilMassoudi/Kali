package kali.microservices.infrastructureservice.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * A client's Nova SSH keypair. Every client shares the same Keystone user (safozi-app), so
 * Nova keypairs are named per that ONE user — {@code openstackName} is a globally-unique,
 * generated name ("u{userId}-{sanitized display name}") to avoid collisions between clients,
 * while {@code displayName} is what the client actually typed. The private key is returned
 * exactly once by Nova on creation and is deliberately NEVER persisted here.
 */
@Entity
@Table(name = "client_keypairs")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ClientKeypair {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private String displayName;

    @Column(unique = true, nullable = false)
    private String openstackName;

    @Column(columnDefinition = "TEXT")
    private String publicKey;

    private String fingerprint;

    @CreationTimestamp
    private LocalDateTime createdAt;
}
