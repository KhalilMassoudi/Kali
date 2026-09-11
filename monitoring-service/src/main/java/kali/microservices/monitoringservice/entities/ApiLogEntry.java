package kali.microservices.monitoringservice.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/** One proxied API request, shipped by the Gateway right after it forwards the request. */
@Entity
@Table(name = "api_logs")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApiLogEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String method;

    @Column(nullable = false, length = 512)
    private String path;

    private String targetService;
    private String userEmail;
    private String userRole;
    private Integer statusCode;
    private Long durationMs;

    private LocalDateTime timestamp;

    @PrePersist
    public void prePersist() {
        if (timestamp == null) {
            timestamp = LocalDateTime.now();
        }
    }
}
