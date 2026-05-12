package kali.microservices.infrastructureservice.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "k8s_clusters")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class K8sCluster {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private String name;

    private String kubernetesVersion; // ex: 1.28
    private Integer nodeCount;
    private Integer cpuPerNode;
    private Integer ramPerNode;       // en MB
    private String region;

    @Enumerated(EnumType.STRING)
    private ClusterStatus status = ClusterStatus.PROVISIONING;

    private String apiEndpoint;
    private String kubeconfig;        // stocké de manière sécurisée

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    public enum ClusterStatus {
        PROVISIONING, RUNNING, SCALING, STOPPED, DELETED, ERROR
    }
}