package kali.microservices.authservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class SmtpConfigDto {
    private String host;
    private Integer port;
    private String username;
    private boolean passwordConfigured;
    private String fromAddress;
    private String fromName;
    private boolean authEnabled;
    private String encryption;
    private LocalDateTime updatedAt;
}
