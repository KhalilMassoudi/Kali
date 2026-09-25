package kali.microservices.monitoringservice.repository;

import kali.microservices.monitoringservice.entities.ApiLogEntry;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ApiLogRepository extends JpaRepository<ApiLogEntry, Long> {

    @Query("SELECT l FROM ApiLogEntry l WHERE " +
            "(:service IS NULL OR l.targetService = :service) AND " +
            "(:errorsOnly = FALSE OR l.statusCode >= 400) AND " +
            "(:search IS NULL OR l.path LIKE CONCAT('%', :search, '%')) " +
            "ORDER BY l.timestamp DESC")
    Page<ApiLogEntry> search(@Param("service") String service,
                              @Param("errorsOnly") Boolean errorsOnly,
                              @Param("search") String search,
                              Pageable pageable);

    @Query("SELECT DISTINCT l.targetService FROM ApiLogEntry l ORDER BY l.targetService")
    List<String> findDistinctTargetServices();

    @Modifying
    @Query("DELETE FROM ApiLogEntry l WHERE l.timestamp < :cutoff")
    int deleteByTimestampBefore(@Param("cutoff") LocalDateTime cutoff);
}
