package kali.microservices.infrastructureservice.repository;

import kali.microservices.infrastructureservice.entities.ClientKeypair;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ClientKeypairRepository extends JpaRepository<ClientKeypair, Long> {
    List<ClientKeypair> findByUserId(Long userId);
}
