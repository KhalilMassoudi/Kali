package kali.microservices.chatservice.controller;

import jakarta.validation.Valid;
import kali.microservices.chatservice.dto.HistoryMessageDto;
import kali.microservices.chatservice.dto.MessageRequest;
import kali.microservices.chatservice.dto.MessageResponse;
import kali.microservices.chatservice.entities.Conversation;
import kali.microservices.chatservice.entities.Message;
import kali.microservices.chatservice.security.AuthContext;
import kali.microservices.chatservice.security.AuthenticatedUser;
import kali.microservices.chatservice.service.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;
    private final AuthContext authContext;

    /**
     * Endpoint principal: envoyer un message en langage naturel.
     *
     * POST /api/chat/message
     * Body: { "message": "Crée un VPS Ubuntu 4GB" }
     */
    @PostMapping("/message")
    public ResponseEntity<MessageResponse> sendMessage(@Valid @RequestBody MessageRequest request,
                                                         @RequestHeader("Authorization") String authHeader) {
        AuthenticatedUser caller = authContext.resolve(authHeader);
        return ResponseEntity.ok(chatService.processMessage(request, caller, authHeader));
    }

    /**
     * Historique plat de tous les messages de l'utilisateur authentifié.
     *
     * GET /api/chat/history
     */
    @GetMapping("/history")
    public ResponseEntity<List<HistoryMessageDto>> getHistory(@RequestHeader("Authorization") String authHeader) {
        AuthenticatedUser caller = authContext.resolve(authHeader);
        return ResponseEntity.ok(chatService.getHistory(caller.userId()));
    }

    /**
     * Liste des conversations de l'utilisateur authentifié.
     *
     * GET /api/chat/conversations
     */
    @GetMapping("/conversations")
    public ResponseEntity<List<Conversation>> getConversations(@RequestHeader("Authorization") String authHeader) {
        AuthenticatedUser caller = authContext.resolve(authHeader);
        return ResponseEntity.ok(chatService.getConversations(caller.userId()));
    }

    /**
     * Messages d'une conversation spécifique (doit appartenir à l'appelant).
     *
     * GET /api/chat/conversations/{id}/messages
     */
    @GetMapping("/conversations/{conversationId}/messages")
    public ResponseEntity<List<Message>> getConversationMessages(@PathVariable Long conversationId,
                                                                   @RequestHeader("Authorization") String authHeader) {
        AuthenticatedUser caller = authContext.resolve(authHeader);
        return ResponseEntity.ok(chatService.getConversationMessages(conversationId, caller.userId()));
    }

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Chat Service is running!");
    }
}
