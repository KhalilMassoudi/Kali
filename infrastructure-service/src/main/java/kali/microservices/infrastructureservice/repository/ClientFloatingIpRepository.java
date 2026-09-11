package kali.microservices.infrastructureservice.repository;

import kali.microservices.infrastructureservice.entities.ClientFloatingIp;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ClientFloatingIpRepository extends JpaRepository<ClientFloatingIp, Long> {
    List<ClientFloatingIp> findByUserId(Long userId);
    Optional<ClientFloatingIp> findByFloatingIpAddress(String floatingIpAddress);
}
