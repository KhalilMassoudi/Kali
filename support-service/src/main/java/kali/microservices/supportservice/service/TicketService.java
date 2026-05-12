package kali.microservices.supportservice.service;

import kali.microservices.supportservice.dto.CreateTicketRequest;
import kali.microservices.supportservice.entities.Ticket;
import kali.microservices.supportservice.entities.TicketComment;
import kali.microservices.supportservice.repository.TicketCommentRepository;
import kali.microservices.supportservice.repository.TicketRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TicketService {

    private final TicketRepository ticketRepository;
    private final TicketCommentRepository commentRepository;

    public Ticket createTicket(CreateTicketRequest request) {
        Ticket ticket = new Ticket();
        ticket.setUserId(request.getUserId());
        ticket.setTitle(request.getTitle());
        ticket.setDescription(request.getDescription());
        ticket.setPriority(request.getPriority());
        ticket.setCategory(request.getCategory());
        ticket.setStatus(Ticket.TicketStatus.OPEN);

        return ticketRepository.save(ticket);
    }

    public Ticket getTicketById(Long id) {
        return ticketRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Ticket non trouvé: " + id));
    }

    public List<Ticket> getTicketsByUser(Long userId) {
        return ticketRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    public List<Ticket> getAllOpenTickets() {
        return ticketRepository.findByStatus(Ticket.TicketStatus.OPEN);
    }

    public Ticket updateTicketStatus(Long id, Ticket.TicketStatus status) {
        Ticket ticket = getTicketById(id);
        ticket.setStatus(status);
        return ticketRepository.save(ticket);
    }

    public Ticket assignTicket(Long ticketId, Long agentId) {
        Ticket ticket = getTicketById(ticketId);
        ticket.setAssignedAgentId(agentId);
        ticket.setStatus(Ticket.TicketStatus.IN_PROGRESS);
        return ticketRepository.save(ticket);
    }

    @Transactional
    public TicketComment addComment(Long ticketId, Long authorId, String content, boolean isAgent) {
        Ticket ticket = getTicketById(ticketId);

        TicketComment comment = new TicketComment();
        comment.setTicket(ticket);
        comment.setAuthorId(authorId);
        comment.setContent(content);
        comment.setAgent(isAgent);

        if (!isAgent) {
            // Si le client répond, remettre en WAITING_RESPONSE pour l'agent
            if (ticket.getStatus() == Ticket.TicketStatus.RESOLVED) {
                ticket.setStatus(Ticket.TicketStatus.OPEN);
                ticketRepository.save(ticket);
            }
        }

        return commentRepository.save(comment);
    }

    public List<TicketComment> getComments(Long ticketId) {
        return commentRepository.findByTicketIdOrderByCreatedAtAsc(ticketId);
    }
}