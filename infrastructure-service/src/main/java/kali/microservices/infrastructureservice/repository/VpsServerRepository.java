package kali.microservices.infrastructureservice.repository;

import kali.microservices.infrastructureservice.entities.VpsServer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VpsServerRepository extends JpaRepository<VpsServer, Long> {
    List<VpsServer> findByUserId(Long userId);
    List<VpsServer> findByUserIdAndStatus(Long userId, VpsServer.VpsStatus status);
    List<VpsServer> findByProjectId(Long projectId);

    @Modifying
    @Query("update VpsServer v set v.projectId = null where v.projectId = :projectId")
    void clearProjectId(Long projectId);
}