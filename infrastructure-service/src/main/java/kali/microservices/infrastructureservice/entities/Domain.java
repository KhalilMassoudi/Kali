package kali.microservices.infrastructureservice.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "domains")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Domain {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false, unique = true)
    private String name;       // ex: monsite.com

    private String tld;        // .com, .fr, .io...

    @Enumerated(EnumType.STRING)
    private DomainStatus status = DomainStatus.PENDING;

    private LocalDate registeredAt;
    private LocalDate expiresAt;

    private String nameserver1;
    private String nameserver2;

    @CreationTimestamp
    private LocalDateTime createdAt;

    public enum DomainStatus {
        PENDING, ACTIVE, EXPIRED, TRANSFERRED, DELETED
    }
}