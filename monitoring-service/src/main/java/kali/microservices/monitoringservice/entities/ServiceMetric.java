package kali.microservices.monitoringservice.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "service_metrics")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ServiceMetric {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String serviceName;   // auth-service, chat-service, etc.

    @Enumerated(EnumType.STRING)
    private ServiceStatus status;

    private Double responseTimeMs;
    private Double cpuUsagePercent;
    private Double memoryUsagePercent;
    private Long requestCount;
    private Long errorCount;

    private LocalDateTime timestamp;

    @PrePersist
    public void prePersist() {
        if (timestamp == null) {
            timestamp = LocalDateTime.now();
        }
    }

    public enum ServiceStatus {
        UP, DOWN, DEGRADED, UNKNOWN
    }
}