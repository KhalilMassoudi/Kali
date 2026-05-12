package kali.microservices.chatservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MessageResponse {

    private String message;           // Réponse textuelle de l'assistant

    private ActionDto action;         // Null si aucune action exécutée

    private Long conversationId;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ActionDto {
        private String type;          // ex: "vps_created", "domain_registered"
        private Object data;          // Objet retourné par le service
    }
}
