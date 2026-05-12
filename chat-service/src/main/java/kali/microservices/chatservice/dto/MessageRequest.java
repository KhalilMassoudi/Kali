package kali.microservices.chatservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class MessageRequest {

    @NotNull(message = "userId est obligatoire")
    private Long userId;

    /** Optionnel: reprendre une conversation existante */
    private Long conversationId;

    @NotBlank(message = "Le message ne peut pas être vide")
    private String message;
}
