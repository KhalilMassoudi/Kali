package kali.microservices.infrastructureservice.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "vps_servers")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class VpsServer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String os;        // ubuntu, debian, centos...

    private Integer ram;      // en MB
    private Integer cpu;      // nombre de vCPU
    private Integer storage;  // en GB

    private String ipAddress;
    private String region;

    @Enumerated(EnumType.STRING)
    private VpsStatus status = VpsStatus.PENDING;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    public enum VpsStatus {
        PENDING, RUNNING, STOPPED, DELETED, ERROR
    }
}