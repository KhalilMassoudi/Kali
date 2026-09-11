package kali.microservices.supportservice.entities;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/** Marks a userId (always an ADMIN in auth-service) as permitted to see/manage every ticket. */
@Entity
@Table(name = "ticket_agents")
@Data
@NoArgsConstructor
public class TicketAgent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private Long userId;

    @Column(nullable = false)
    private Long grantedByUserId;

    @CreationTimestamp
    private LocalDateTime grantedAt;

    public TicketAgent(Long userId, Long grantedByUserId) {
        this.userId = userId;
        this.grantedByUserId = grantedByUserId;
    }
}
