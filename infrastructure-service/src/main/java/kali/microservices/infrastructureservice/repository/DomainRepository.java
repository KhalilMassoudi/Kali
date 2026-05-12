package kali.microservices.infrastructureservice.repository;

import kali.microservices.infrastructureservice.entities.Domain;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DomainRepository extends JpaRepository<Domain, Long> {
    List<Domain> findByUserId(Long userId);
    Optional<Domain> findByName(String name);
    boolean existsByName(String name);
}