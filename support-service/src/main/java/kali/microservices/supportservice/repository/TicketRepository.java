package kali.microservices.supportservice.repository;

import kali.microservices.supportservice.entities.Ticket;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TicketRepository extends JpaRepository<Ticket, Long> {
    List<Ticket> findByUserIdOrderByCreatedAtDesc(Long userId);
    List<Ticket> findByUserIdAndStatus(Long userId, Ticket.TicketStatus status);
    List<Ticket> findByStatus(Ticket.TicketStatus status);
    List<Ticket> findByAssignedAgentId(Long agentId);
}