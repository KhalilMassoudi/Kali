package kali.microservices.chatservice.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class MessageRequest {

    /** Optionnel: reprendre une conversation existante */
    private Long conversationId;

    @NotBlank(message = "Le message ne peut pas être vide")
    private String message;
}
