package kali.microservices.supportservice.service;

import kali.microservices.supportservice.entities.TicketAgent;
import kali.microservices.supportservice.repository.TicketAgentRepository;
import kali.microservices.supportservice.security.AuthenticatedUser;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/**
 * Ticket-management access is deliberately NOT automatic for every ADMIN — it's an explicit
 * per-admin grant recorded locally here (support-service owns this table, no dependency on
 * auth-service's DB). Being ADMIN is still required in addition to the grant, so a stale
 * TicketAgent row left behind after an unrelated demotion in auth-service never over-grants.
 */
@Service
@RequiredArgsConstructor
public class TicketAgentService {

    private final TicketAgentRepository ticketAgentRepository;

    public boolean isPermittedAgent(Long userId) {
        return userId != null && ticketAgentRepository.existsByUserId(userId);
    }

    public void requireAgent(AuthenticatedUser user) {
        if (!(user.isAdmin() && isPermittedAgent(user.userId()))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Permission de gestion des tickets requise");
        }
    }

    public void requireOwnerOrAgent(AuthenticatedUser user, Long resourceOwnerId) {
        boolean isOwner = user.userId() != null && user.userId().equals(resourceOwnerId);
        boolean isAgent = user.isAdmin() && isPermittedAgent(user.userId());
        if (!isOwner && !isAgent) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Accès refusé");
        }
    }

    public TicketAgent grant(Long targetUserId, Long grantedByUserId) {
        return ticketAgentRepository.findByUserId(targetUserId)
                .orElseGet(() -> ticketAgentRepository.save(new TicketAgent(targetUserId, grantedByUserId)));
    }

    public void revoke(Long targetUserId) {
        ticketAgentRepository.deleteByUserId(targetUserId);
    }

    public List<TicketAgent> listAgents() {
        return ticketAgentRepository.findAllByOrderByGrantedAtDesc();
    }
}
