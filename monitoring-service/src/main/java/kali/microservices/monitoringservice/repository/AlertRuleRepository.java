package kali.microservices.monitoringservice.repository;

import kali.microservices.monitoringservice.entities.AlertRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AlertRuleRepository extends JpaRepository<AlertRule, Long> {
    List<AlertRule> findByUserIdAndEnabled(Long userId, boolean enabled);
    List<AlertRule> findByEnabled(boolean enabled);
}