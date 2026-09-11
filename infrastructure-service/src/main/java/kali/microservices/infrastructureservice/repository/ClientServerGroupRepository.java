package kali.microservices.infrastructureservice.repository;

import kali.microservices.infrastructureservice.entities.ClientServerGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ClientServerGroupRepository extends JpaRepository<ClientServerGroup, Long> {
    List<ClientServerGroup> findByUserId(Long userId);
}
