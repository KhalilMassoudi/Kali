package kali.microservices.monitoringservice.repository;

import kali.microservices.monitoringservice.entities.AlertEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AlertEventRepository extends JpaRepository<AlertEvent, Long> {
    List<AlertEvent> findByResolvedFalseOrderByFiredAtDesc();
    boolean existsByRuleIdAndServiceNameAndResolvedFalse(Long ruleId, String serviceName);
}
