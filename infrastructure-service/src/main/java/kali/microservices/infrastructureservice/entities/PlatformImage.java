package kali.microservices.infrastructureservice.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Admin-curated catalog of OS images clients can pick from when creating a VM — replaces the
 * old hardcoded 4-entry OS list. A row always points at a real Glance image (externalId);
 * this table only tracks display metadata and publish state.
 */
@Entity
@Table(name = "platform_images")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PlatformImage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String externalId; // Glance image UUID

    @Column(nullable = false)
    private String displayName;

    private String osDistro;
    private String osVersion;
    private Integer minDiskGb;
    private Integer minRamMb;

    @Enumerated(EnumType.STRING)
    private ImageVisibility visibility = ImageVisibility.HIDDEN;

    @Enumerated(EnumType.STRING)
    private ImageSource source;

    @CreationTimestamp
    private LocalDateTime createdAt;

    public enum ImageVisibility {
        PUBLISHED, HIDDEN
    }

    public enum ImageSource {
        ADOPTED, IMPORTED_URL, UPLOADED
    }
}
