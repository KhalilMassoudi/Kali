package kali.microservices.infrastructureservice.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "vps_backup_schedules")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class VpsBackupSchedule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private Long vpsId;

    @Column(nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    private Frequency frequency = Frequency.DAILY;

    private Integer retentionCount = 3;
    private boolean enabled = true;

    private LocalDateTime lastRunAt;
    private LocalDateTime nextRunAt;

    public enum Frequency {
        DAILY, WEEKLY
    }
}
