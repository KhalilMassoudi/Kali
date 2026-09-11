package kali.microservices.chatservice.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import kali.microservices.chatservice.dto.HistoryMessageDto;
import kali.microservices.chatservice.dto.MessageRequest;
import kali.microservices.chatservice.dto.MessageResponse;
import kali.microservices.chatservice.entities.Conversation;
import kali.microservices.chatservice.entities.Message;
import kali.microservices.chatservice.repository.ConversationRepository;
import kali.microservices.chatservice.repository.MessageRepository;
import kali.microservices.chatservice.security.AuthenticatedUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatService {

    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;
    private final OllamaAgentService ollamaAgentService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Transactional
    public MessageResponse processMessage(MessageRequest request, AuthenticatedUser caller, String authHeader) {

        // 1. Récupérer ou créer la conversation
        Conversation conversation = getOrCreateConversation(caller.userId(), request.getConversationId());

        // 2. Sauvegarder le message utilisateur
        Message userMsg = saveMessage(conversation, Message.MessageRole.USER, request.getMessage(), null, null);

        // 3. Construire l'historique (sans le message qu'on vient de sauver)
        List<Map<String, String>> history = buildHistory(conversation.getId(), userMsg.getId());

        // 4. Appel de l'agent IA (modèle local Ollama + function calling natif)
        OllamaAgentService.AgentResult result = ollamaAgentService.chat(request.getMessage(), history, caller.userId(), authHeader);
        String assistantText = result.text();
        Object actionData = result.lastActionData();
        log.info("Action exécutée: {}", result.lastAction());

        // 7. Sauvegarder la réponse assistant
        saveMessage(conversation, Message.MessageRole.ASSISTANT, assistantText, result.lastAction(), actionData);

        // 8. Mettre à jour le titre de la conversation (premier message)
        if (conversation.getTitle() == null) {
            String title = request.getMessage().length() > 50
                    ? request.getMessage().substring(0, 50) + "..."
                    : request.getMessage();
            conversation.setTitle(title);
            conversationRepository.save(conversation);
        }

        // 9. Construire la réponse HTTP
        MessageResponse.ActionDto actionDto = actionData != null
                ? new MessageResponse.ActionDto(result.lastAction(), actionData)
                : null;

        return new MessageResponse(assistantText, actionDto, conversation.getId());
    }

    public List<HistoryMessageDto> getHistory(Long userId) {
        List<Conversation> conversations = conversationRepository.findByUserIdOrderByUpdatedAtDesc(userId);
        List<HistoryMessageDto> history = new ArrayList<>();

        for (Conversation conv : conversations) {
            List<Message> messages = messageRepository.findByConversationIdOrderByCreatedAtAsc(conv.getId());
            for (Message msg : messages) {
                String role = msg.getRole() == Message.MessageRole.USER ? "user" : "assistant";
                history.add(new HistoryMessageDto(role, msg.getContent(), msg.getDetectedAction(), msg.getCreatedAt()));
            }
        }
        return history;
    }

    public List<Conversation> getConversations(Long userId) {
        return conversationRepository.findByUserIdOrderByUpdatedAtDesc(userId);
    }

    public List<Message> getConversationMessages(Long conversationId, Long callerId) {
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Conversation introuvable: " + conversationId));
        if (!conversation.getUserId().equals(callerId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Accès refusé");
        }
        return messageRepository.findByConversationIdOrderByCreatedAtAsc(conversationId);
    }

    // ─────────────────────────────── Helpers ────────────────────────────────

    private Conversation getOrCreateConversation(Long userId, Long conversationId) {
        if (conversationId != null) {
            Conversation conversation = conversationRepository.findById(conversationId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Conversation introuvable: " + conversationId));
            if (!conversation.getUserId().equals(userId)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Accès refusé");
            }
            return conversation;
        }
        Conversation conv = new Conversation();
        conv.setUserId(userId);
        return conversationRepository.save(conv);
    }

    private Message saveMessage(Conversation conv, Message.MessageRole role,
                                 String content, String action, Object actionData) {
        Message msg = new Message();
        msg.setConversation(conv);
        msg.setRole(role);
        msg.setContent(content);
        msg.setDetectedAction(action);

        if (actionData != null) {
            try {
                msg.setActionResult(objectMapper.writeValueAsString(actionData));
            } catch (Exception ignored) {}
        }
        return messageRepository.save(msg);
    }

    private List<Map<String, String>> buildHistory(Long conversationId, Long excludeId) {
        List<Message> messages = messageRepository.findByConversationIdOrderByCreatedAtAsc(conversationId);
        List<Map<String, String>> history = new ArrayList<>();
        for (Message msg : messages) {
            if (msg.getId().equals(excludeId)) continue;
            String role = msg.getRole() == Message.MessageRole.USER ? "user" : "assistant";
            history.add(Map.of("role", role, "content", msg.getContent()));
        }
        return history;
    }
}
