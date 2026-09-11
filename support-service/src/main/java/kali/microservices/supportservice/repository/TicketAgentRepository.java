package kali.microservices.supportservice.repository;

import kali.microservices.supportservice.entities.TicketAgent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TicketAgentRepository extends JpaRepository<TicketAgent, Long> {
    boolean existsByUserId(Long userId);
    Optional<TicketAgent> findByUserId(Long userId);
    List<TicketAgent> findAllByOrderByGrantedAtDesc();
    void deleteByUserId(Long userId);
}
