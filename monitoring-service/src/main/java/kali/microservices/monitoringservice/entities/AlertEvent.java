package kali.microservices.monitoringservice.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "alert_events")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AlertEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long ruleId;

    private String serviceName;

    @Enumerated(EnumType.STRING)
    private AlertRule.AlertType alertType;

    private String message;

    @Enumerated(EnumType.STRING)
    private Severity severity;

    private LocalDateTime firedAt;

    private boolean resolved = false;

    private LocalDateTime resolvedAt;

    @PrePersist
    public void prePersist() {
        if (firedAt == null) {
            firedAt = LocalDateTime.now();
        }
    }

    public enum Severity {
        INFO, WARNING, CRITICAL
    }
}
