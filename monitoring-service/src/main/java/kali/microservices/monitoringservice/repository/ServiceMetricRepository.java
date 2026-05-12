package kali.microservices.monitoringservice.repository;

import kali.microservices.monitoringservice.entities.ServiceMetric;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ServiceMetricRepository extends JpaRepository<ServiceMetric, Long> {
    List<ServiceMetric> findByServiceNameOrderByTimestampDesc(String serviceName);
    List<ServiceMetric> findByServiceNameAndTimestampBetween(String serviceName, LocalDateTime from, LocalDateTime to);

    @Query("SELECT m FROM ServiceMetric m WHERE m.timestamp = (SELECT MAX(m2.timestamp) FROM ServiceMetric m2 WHERE m2.serviceName = m.serviceName)")
    List<ServiceMetric> findLatestMetricPerService();

    Optional<ServiceMetric> findTopByServiceNameOrderByTimestampDesc(String serviceName);
}
