package kali.microservices.infrastructureservice.repository;

import kali.microservices.infrastructureservice.entities.VpsNetwork;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VpsNetworkRepository extends JpaRepository<VpsNetwork, Long> {
    List<VpsNetwork> findByUserId(Long userId);
    List<VpsNetwork> findByProjectId(Long projectId);

    @Modifying
    @Query("update VpsNetwork n set n.projectId = null where n.projectId = :projectId")
    void clearProjectId(Long projectId);
}
