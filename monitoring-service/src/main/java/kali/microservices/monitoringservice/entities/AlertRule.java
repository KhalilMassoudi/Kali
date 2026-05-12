package kali.microservices.monitoringservice.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "alert_rules")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AlertRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    private String serviceName;   // null = tous les services

    @Enumerated(EnumType.STRING)
    private AlertType alertType;

    private Double threshold;     // seuil de déclenchement (%)

    private boolean enabled = true;

    @CreationTimestamp
    private LocalDateTime createdAt;

    public enum AlertType {
        CPU_HIGH, MEMORY_HIGH, RESPONSE_TIME_HIGH, SERVICE_DOWN, ERROR_RATE_HIGH
    }
}