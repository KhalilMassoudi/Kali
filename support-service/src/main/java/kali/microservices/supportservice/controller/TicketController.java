package kali.microservices.supportservice.controller;

import jakarta.validation.Valid;
import kali.microservices.supportservice.dto.CreateTicketRequest;
import kali.microservices.supportservice.entities.Ticket;
import kali.microservices.supportservice.entities.TicketComment;
import kali.microservices.supportservice.service.TicketService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/support")
@RequiredArgsConstructor
public class TicketController {

    private final TicketService ticketService;

    @PostMapping("/tickets")
    public ResponseEntity<Ticket> createTicket(@Valid @RequestBody CreateTicketRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ticketService.createTicket(request));
    }

    @GetMapping("/tickets/{id}")
    public ResponseEntity<Ticket> getTicket(@PathVariable Long id) {
        return ResponseEntity.ok(ticketService.getTicketById(id));
    }

    @GetMapping("/tickets/user/{userId}")
    public ResponseEntity<List<Ticket>> getTicketsByUser(@PathVariable Long userId) {
        return ResponseEntity.ok(ticketService.getTicketsByUser(userId));
    }

    @GetMapping("/tickets/open")
    public ResponseEntity<List<Ticket>> getOpenTickets() {
        return ResponseEntity.ok(ticketService.getAllOpenTickets());
    }

    @PatchMapping("/tickets/{id}/status")
    public ResponseEntity<Ticket> updateStatus(@PathVariable Long id,
                                                @RequestParam Ticket.TicketStatus status) {
        return ResponseEntity.ok(ticketService.updateTicketStatus(id, status));
    }

    @PatchMapping("/tickets/{id}/assign")
    public ResponseEntity<Ticket> assignTicket(@PathVariable Long id,
                                                @RequestParam Long agentId) {
        return ResponseEntity.ok(ticketService.assignTicket(id, agentId));
    }

    @PostMapping("/tickets/{id}/comments")
    public ResponseEntity<TicketComment> addComment(@PathVariable Long id,
                                                     @RequestBody Map<String, Object> body) {
        Long authorId = Long.valueOf(body.get("authorId").toString());
        String content = body.get("content").toString();
        boolean isAgent = body.containsKey("isAgent") && Boolean.parseBoolean(body.get("isAgent").toString());

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ticketService.addComment(id, authorId, content, isAgent));
    }

    @GetMapping("/tickets/{id}/comments")
    public ResponseEntity<List<TicketComment>> getComments(@PathVariable Long id) {
        return ResponseEntity.ok(ticketService.getComments(id));
    }

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Support Service is running!");
    }
}