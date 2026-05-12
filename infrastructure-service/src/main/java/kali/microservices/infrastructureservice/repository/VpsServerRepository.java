package kali.microservices.infrastructureservice.repository;

import kali.microservices.infrastructureservice.entities.VpsServer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VpsServerRepository extends JpaRepository<VpsServer, Long> {
    List<VpsServer> findByUserId(Long userId);
    List<VpsServer> findByUserIdAndStatus(Long userId, VpsServer.VpsStatus status);
}