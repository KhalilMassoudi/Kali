package kali.microservices.billingservice.repository;

import kali.microservices.billingservice.entities.UsageRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface UsageRecordRepository extends JpaRepository<UsageRecord, Long> {
    List<UsageRecord> findByUserIdAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(Long userId, LocalDateTime from, LocalDateTime to);
}
