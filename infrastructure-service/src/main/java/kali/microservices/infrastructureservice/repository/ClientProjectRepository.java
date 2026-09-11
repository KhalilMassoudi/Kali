package kali.microservices.infrastructureservice.repository;

import kali.microservices.infrastructureservice.entities.ClientProject;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ClientProjectRepository extends JpaRepository<ClientProject, Long> {
    List<ClientProject> findByUserId(Long userId);
    boolean existsByUserIdAndNameIgnoreCase(Long userId, String name);
}
