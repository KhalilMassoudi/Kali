package kali.microservices.supportservice.controller;

import jakarta.validation.Valid;
import kali.microservices.supportservice.dto.AddCommentRequest;
import kali.microservices.supportservice.dto.AttachmentDto;
import kali.microservices.supportservice.dto.CreateTicketForClientRequest;
import kali.microservices.supportservice.dto.CreateTicketRequest;
import kali.microservices.supportservice.entities.Ticket;
import kali.microservices.supportservice.entities.TicketAgent;
import kali.microservices.supportservice.entities.TicketAttachment;
import kali.microservices.supportservice.entities.TicketComment;
import kali.microservices.supportservice.security.AuthContext;
import kali.microservices.supportservice.security.AuthenticatedUser;
import kali.microservices.supportservice.service.TicketAgentService;
import kali.microservices.supportservice.service.TicketAttachmentService;
import kali.microservices.supportservice.service.TicketService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/support")
@RequiredArgsConstructor
public class TicketController {

    private final TicketService ticketService;
    private final TicketAgentService ticketAgentService;
    private final TicketAttachmentService attachmentService;
    private final AuthContext authContext;

    @PostMapping("/tickets")
    public ResponseEntity<Ticket> createTicket(@Valid @RequestBody CreateTicketRequest request,
                                                @RequestHeader("Authorization") String authHeader) {
        AuthenticatedUser caller = authContext.resolve(authHeader);
        return ResponseEntity.status(HttpStatus.CREATED).body(ticketService.createTicket(caller, request));
    }

    @GetMapping("/tickets/{id}")
    public ResponseEntity<Ticket> getTicket(@PathVariable Long id,
                                             @RequestHeader("Authorization") String authHeader) {
        AuthenticatedUser caller = authContext.resolve(authHeader);
        Ticket ticket = ticketService.getTicketById(id);
        ticketAgentService.requireOwnerOrAgent(caller, ticket.getUserId());
        return ResponseEntity.ok(ticket);
    }

    @GetMapping("/tickets/user/{userId}")
    public ResponseEntity<List<Ticket>> getTicketsByUser(@PathVariable Long userId,
                                                          @RequestHeader("Authorization") String authHeader) {
        AuthenticatedUser caller = authContext.resolve(authHeader);
        ticketAgentService.requireOwnerOrAgent(caller, userId);
        return ResponseEntity.ok(ticketService.getTicketsByUser(userId));
    }

    @GetMapping("/tickets/open")
    public ResponseEntity<List<Ticket>> getOpenTickets(@RequestHeader("Authorization") String authHeader) {
        AuthenticatedUser caller = authContext.resolve(authHeader);
        ticketAgentService.requireAgent(caller);
        return ResponseEntity.ok(ticketService.getAllOpenTickets());
    }

    @GetMapping("/admin/tickets")
    public ResponseEntity<List<Ticket>> getAllTickets(@RequestParam(required = false) Ticket.TicketStatus status,
                                                        @RequestParam(required = false) Ticket.Priority priority,
                                                        @RequestParam(required = false) Ticket.Category category,
                                                        @RequestHeader("Authorization") String authHeader) {
        AuthenticatedUser caller = authContext.resolve(authHeader);
        ticketAgentService.requireAgent(caller);
        return ResponseEntity.ok(ticketService.getAllTickets(status, priority, category));
    }

    @PostMapping("/admin/tickets")
    public ResponseEntity<Ticket> createTicketForClient(@Valid @RequestBody CreateTicketForClientRequest request,
                                                          @RequestHeader("Authorization") String authHeader) {
        AuthenticatedUser caller = authContext.resolve(authHeader);
        ticketAgentService.requireAgent(caller);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ticketService.createTicketForClient(authHeader, request));
    }

    @PatchMapping("/tickets/{id}/status")
    public ResponseEntity<Ticket> updateStatus(@PathVariable Long id,
                                                @RequestParam Ticket.TicketStatus status,
                                                @RequestHeader("Authorization") String authHeader) {
        AuthenticatedUser caller = authContext.resolve(authHeader);
        Ticket ticket = ticketService.getTicketById(id);
        if (status == Ticket.TicketStatus.CLOSED) {
            ticketAgentService.requireOwnerOrAgent(caller, ticket.getUserId());
        } else {
            ticketAgentService.requireAgent(caller);
        }
        return ResponseEntity.ok(ticketService.updateTicketStatus(id, status));
    }

    @PatchMapping("/tickets/{id}/assign")
    public ResponseEntity<Ticket> assignTicket(@PathVariable Long id,
                                                @RequestParam Long agentId,
                                                @RequestHeader("Authorization") String authHeader) {
        AuthenticatedUser caller = authContext.resolve(authHeader);
        ticketAgentService.requireAgent(caller);
        return ResponseEntity.ok(ticketService.assignTicket(id, agentId));
    }

    @PostMapping("/tickets/{id}/comments")
    public ResponseEntity<TicketComment> addComment(@PathVariable Long id,
                                                      @Valid @RequestBody AddCommentRequest request,
                                                      @RequestHeader("Authorization") String authHeader) {
        AuthenticatedUser caller = authContext.resolve(authHeader);
        Ticket ticket = ticketService.getTicketById(id);
        ticketAgentService.requireOwnerOrAgent(caller, ticket.getUserId());
        boolean isAgent = caller.isAdmin() && ticketAgentService.isPermittedAgent(caller.userId());

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ticketService.addComment(id, caller, isAgent, request.getContent(), authHeader));
    }

    @GetMapping("/tickets/{id}/comments")
    public ResponseEntity<List<TicketComment>> getComments(@PathVariable Long id,
                                                             @RequestHeader("Authorization") String authHeader) {
        AuthenticatedUser caller = authContext.resolve(authHeader);
        Ticket ticket = ticketService.getTicketById(id);
        ticketAgentService.requireOwnerOrAgent(caller, ticket.getUserId());
        return ResponseEntity.ok(ticketService.getComments(id));
    }

    @PostMapping(value = "/tickets/{id}/attachments", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<AttachmentDto> uploadAttachment(@PathVariable Long id,
                                                           @RequestParam(required = false) Long commentId,
                                                           @RequestParam("file") MultipartFile file,
                                                           @RequestHeader("Authorization") String authHeader) {
        AuthenticatedUser caller = authContext.resolve(authHeader);
        Ticket ticket = ticketService.getTicketById(id);
        ticketAgentService.requireOwnerOrAgent(caller, ticket.getUserId());
        TicketAttachment attachment = attachmentService.upload(id, commentId, caller.userId(), file);
        return ResponseEntity.status(HttpStatus.CREATED).body(AttachmentDto.from(attachment));
    }

    @GetMapping("/tickets/{id}/attachments")
    public ResponseEntity<List<AttachmentDto>> listAttachments(@PathVariable Long id,
                                                                 @RequestHeader("Authorization") String authHeader) {
        AuthenticatedUser caller = authContext.resolve(authHeader);
        Ticket ticket = ticketService.getTicketById(id);
        ticketAgentService.requireOwnerOrAgent(caller, ticket.getUserId());
        return ResponseEntity.ok(attachmentService.listForTicket(id));
    }

    @GetMapping("/attachments/{attachmentId}/download")
    public ResponseEntity<byte[]> downloadAttachment(@PathVariable Long attachmentId,
                                                      @RequestHeader("Authorization") String authHeader) {
        AuthenticatedUser caller = authContext.resolve(authHeader);
        TicketAttachment attachment = attachmentService.get(attachmentId);
        Ticket ticket = ticketService.getTicketById(attachment.getTicketId());
        ticketAgentService.requireOwnerOrAgent(caller, ticket.getUserId());
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(attachment.getContentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + attachment.getFilename() + "\"")
                .body(attachment.getData());
    }

    @PostMapping("/agents/{userId}")
    public ResponseEntity<TicketAgent> grantAgent(@PathVariable Long userId,
                                                   @RequestHeader("Authorization") String authHeader) {
        AuthenticatedUser caller = authContext.requireAdmin(authHeader);
        return ResponseEntity.ok(ticketAgentService.grant(userId, caller.userId()));
    }

    @DeleteMapping("/agents/{userId}")
    public ResponseEntity<Void> revokeAgent(@PathVariable Long userId,
                                             @RequestHeader("Authorization") String authHeader) {
        authContext.requireAdmin(authHeader);
        ticketAgentService.revoke(userId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/agents")
    public ResponseEntity<List<TicketAgent>> listAgents(@RequestHeader("Authorization") String authHeader) {
        authContext.requireAdmin(authHeader);
        return ResponseEntity.ok(ticketAgentService.listAgents());
    }

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Support Service is running!");
    }
}
