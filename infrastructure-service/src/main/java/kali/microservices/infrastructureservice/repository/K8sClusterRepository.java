package kali.microservices.infrastructureservice.repository;

import kali.microservices.infrastructureservice.entities.K8sCluster;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface K8sClusterRepository extends JpaRepository<K8sCluster, Long> {
    List<K8sCluster> findByUserId(Long userId);
    List<K8sCluster> findByUserIdAndStatus(Long userId, K8sCluster.ClusterStatus status);
}