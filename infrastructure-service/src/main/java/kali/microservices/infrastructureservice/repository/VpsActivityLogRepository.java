package kali.microservices.infrastructureservice.repository;

import kali.microservices.infrastructureservice.entities.VpsActivityLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface VpsActivityLogRepository extends JpaRepository<VpsActivityLog, Long> {
    Page<VpsActivityLog> findByVpsIdOrderByCreatedAtDesc(Long vpsId, Pageable pageable);
}
