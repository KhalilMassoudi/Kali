package kali.microservices.infrastructureservice.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * A client's Cinder generic volume group (raw REST — see CinderGroupClient). Best-effort:
 * this Cinder extension may not be enabled on every OpenStack deployment; the frontend
 * feature-detects and hides this UI on failure rather than assuming support.
 */
@Entity
@Table(name = "client_volume_groups")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ClientVolumeGroup {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private String name;

    private String description;

    @Column(unique = true)
    private String externalId; // Cinder group UUID

    @CreationTimestamp
    private LocalDateTime createdAt;
}
