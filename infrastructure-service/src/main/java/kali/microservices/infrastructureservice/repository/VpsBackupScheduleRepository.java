package kali.microservices.infrastructureservice.repository;

import kali.microservices.infrastructureservice.entities.VpsBackupSchedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface VpsBackupScheduleRepository extends JpaRepository<VpsBackupSchedule, Long> {
    Optional<VpsBackupSchedule> findByVpsId(Long vpsId);
    List<VpsBackupSchedule> findByEnabledTrueAndNextRunAtLessThanEqual(LocalDateTime now);
}
