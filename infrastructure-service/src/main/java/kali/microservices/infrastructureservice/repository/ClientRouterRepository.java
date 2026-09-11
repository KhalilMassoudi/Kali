package kali.microservices.infrastructureservice.repository;

import kali.microservices.infrastructureservice.entities.ClientRouter;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ClientRouterRepository extends JpaRepository<ClientRouter, Long> {
    List<ClientRouter> findByUserId(Long userId);
}
