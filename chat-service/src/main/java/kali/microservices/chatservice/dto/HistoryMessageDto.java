package kali.microservices.chatservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class HistoryMessageDto {
    private String role;              // "user" ou "assistant"
    private String content;
    private String action;            // action détectée (peut être null)
    private LocalDateTime timestamp;
}
