package kali.microservices.infrastructureservice.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * App-level audit trail of every action taken on a VPS, tagged with the real platform user
 * (from the JWT) who did it. Deliberately not sourced from Nova's own instance-action log:
 * every VM in this platform runs under one shared OpenStack service account, so Nova's log
 * would only ever show that service account as the actor, never the actual client.
 */
@Entity
@Table(name = "vps_activity_logs")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class VpsActivityLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long vpsId;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private String action;

    private String detail;

    @CreationTimestamp
    private LocalDateTime createdAt;
}
