package kali.microservices.infrastructureservice.repository;

import kali.microservices.infrastructureservice.entities.UserQuota;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserQuotaRepository extends JpaRepository<UserQuota, Long> {
    Optional<UserQuota> findByUserId(Long userId);
}
