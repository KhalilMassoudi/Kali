package kali.microservices.chatservice.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "messages")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Message {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "conversation_id", nullable = false)
    private Conversation conversation;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MessageRole role;   // USER ou ASSISTANT

    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    // Action cloud détectée par OpenAI (optionnel)
    private String detectedAction;   // ex: create_vps, list_domains...
    private String actionResult;     // résultat de l'action (JSON)

    @CreationTimestamp
    private LocalDateTime createdAt;

    public enum MessageRole {
        USER, ASSISTANT, SYSTEM
    }
}