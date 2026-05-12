package kali.microservices.chatservice.controller;

import jakarta.validation.Valid;
import kali.microservices.chatservice.dto.HistoryMessageDto;
import kali.microservices.chatservice.dto.MessageRequest;
import kali.microservices.chatservice.dto.MessageResponse;
import kali.microservices.chatservice.entities.Conversation;
import kali.microservices.chatservice.entities.Message;
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

    /**
     * Endpoint principal: envoyer un message en langage naturel.
     *
     * POST /api/chat/message
     * Body: { "userId": 1, "message": "Crée un VPS Ubuntu 4GB" }
     */
    @PostMapping("/message")
    public ResponseEntity<MessageResponse> sendMessage(@Valid @RequestBody MessageRequest request) {
        return ResponseEntity.ok(chatService.processMessage(request));
    }

    /**
     * Historique plat de tous les messages d'un utilisateur.
     *
     * GET /api/chat/history?userId=1
     */
    @GetMapping("/history")
    public ResponseEntity<List<HistoryMessageDto>> getHistory(@RequestParam Long userId) {
        return ResponseEntity.ok(chatService.getHistory(userId));
    }

    /**
     * Liste des conversations d'un utilisateur.
     *
     * GET /api/chat/conversations?userId=1
     */
    @GetMapping("/conversations")
    public ResponseEntity<List<Conversation>> getConversations(@RequestParam Long userId) {
        return ResponseEntity.ok(chatService.getConversations(userId));
    }

    /**
     * Messages d'une conversation spécifique.
     *
     * GET /api/chat/conversations/{id}/messages
     */
    @GetMapping("/conversations/{conversationId}/messages")
    public ResponseEntity<List<Message>> getConversationMessages(@PathVariable Long conversationId) {
        return ResponseEntity.ok(chatService.getConversationMessages(conversationId));
    }

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Chat Service is running!");
    }
}
