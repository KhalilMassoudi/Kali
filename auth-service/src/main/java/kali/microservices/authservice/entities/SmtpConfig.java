package kali.microservices.authservice.entities;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "smtp_config")
@Data
@NoArgsConstructor
public class SmtpConfig {
    @Id
    private Long id = 1L;

    private String host;
    private Integer port;
    private String username;

    @Column(length = 1024)
    private String encryptedPassword;

    private String fromAddress;
    private String fromName;
    private boolean authEnabled = true;

    @Enumerated(EnumType.STRING)
    private Encryption encryption = Encryption.STARTTLS;

    private LocalDateTime updatedAt;

    public enum Encryption {
        NONE, STARTTLS, SSL
    }
}