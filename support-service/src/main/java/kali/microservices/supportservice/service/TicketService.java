package kali.microservices.supportservice.service;

import kali.microservices.supportservice.client.AuthNotificationClient;
import kali.microservices.supportservice.dto.CreateTicketForClientRequest;
import kali.microservices.supportservice.dto.CreateTicketRequest;
import kali.microservices.supportservice.entities.Ticket;
import kali.microservices.supportservice.entities.TicketComment;
import kali.microservices.supportservice.repository.TicketCommentRepository;
import kali.microservices.supportservice.repository.TicketRepository;
import kali.microservices.supportservice.security.AuthenticatedUser;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TicketService {

    private final TicketRepository ticketRepository;
    private final TicketCommentRepository commentRepository;
    private final TicketAgentService ticketAgentService;
    private final AuthNotificationClient authNotificationClient;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    public Ticket createTicket(AuthenticatedUser caller, CreateTicketRequest request) {
        Ticket ticket = new Ticket();
        ticket.setUserId(caller.userId());
        ticket.setTitle(request.getTitle());
        ticket.setDescription(request.getDescription());
        ticket.setPriority(request.getPriority());
        ticket.setCategory(request.getCategory());
        ticket.setStatus(Ticket.TicketStatus.OPEN);

        return ticketRepository.save(ticket);
    }

    public Ticket createTicketForClient(String authHeader, CreateTicketForClientRequest request) {
        Ticket ticket = new Ticket();
        ticket.setUserId(request.getUserId());
        ticket.setTitle(request.getTitle());
        ticket.setDescription(request.getDescription());
        ticket.setPriority(request.getPriority());
        ticket.setCategory(request.getCategory());
        ticket.setStatus(Ticket.TicketStatus.OPEN);

        Ticket saved = ticketRepository.save(ticket);

        authNotificationClient.notifyUser(authHeader, saved.getUserId(),
                "Un ticket a été ouvert pour vous - Safozi",
                "Un ticket \"" + saved.getTitle() + "\" a été ouvert par notre équipe support. "
                        + "Consultez-le ici : " + frontendUrl + "/support/" + saved.getId());

        return saved;
    }

    public Ticket getTicketById(Long id) {
        return ticketRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Ticket non trouvé: " + id));
    }

    public List<Ticket> getTicketsByUser(Long userId) {
        return ticketRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    public List<Ticket> getAllOpenTickets() {
        return ticketRepository.findByStatus(Ticket.TicketStatus.OPEN);
    }

    public List<Ticket> getAllTickets(Ticket.TicketStatus status, Ticket.Priority priority, Ticket.Category category) {
        return ticketRepository.findAll().stream()
                .filter(t -> status == null || t.getStatus() == status)
                .filter(t -> priority == null || t.getPriority() == priority)
                .filter(t -> category == null || t.getCategory() == category)
                .toList();
    }

    public Ticket updateTicketStatus(Long id, Ticket.TicketStatus status) {
        Ticket ticket = getTicketById(id);
        ticket.setStatus(status);
        return ticketRepository.save(ticket);
    }

    public Ticket assignTicket(Long ticketId, Long agentId) {
        if (!ticketAgentService.isPermittedAgent(agentId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "L'utilisateur cible n'a pas la permission de gestion des tickets");
        }
        Ticket ticket = getTicketById(ticketId);
        ticket.setAssignedAgentId(agentId);
        ticket.setStatus(Ticket.TicketStatus.IN_PROGRESS);
        return ticketRepository.save(ticket);
    }

    @Transactional
    public TicketComment addComment(Long ticketId, AuthenticatedUser caller, boolean isAgent, String content, String authHeader) {
        Ticket ticket = getTicketById(ticketId);

        TicketComment comment = new TicketComment();
        comment.setTicket(ticket);
        comment.setAuthorId(caller.userId());
        comment.setContent(content);
        comment.setAgent(isAgent);

        if (isAgent) {
            // Un agent répond : le ticket attend maintenant une réponse du client
            if (ticket.getStatus() == Ticket.TicketStatus.OPEN || ticket.getStatus() == Ticket.TicketStatus.IN_PROGRESS) {
                ticket.setStatus(Ticket.TicketStatus.WAITING_RESPONSE);
                ticketRepository.save(ticket);
            }
            authNotificationClient.notifyUser(authHeader, ticket.getUserId(),
                    "Nouvelle réponse sur votre ticket #" + ticket.getId() + " - Safozi",
                    "Un agent a répondu à votre ticket \"" + ticket.getTitle() + "\". "
                            + "Consultez-le ici : " + frontendUrl + "/support/" + ticket.getId());
        } else {
            // Si le client répond, remettre en OPEN pour l'agent
            if (ticket.getStatus() == Ticket.TicketStatus.RESOLVED || ticket.getStatus() == Ticket.TicketStatus.WAITING_RESPONSE) {
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
