package kali.microservices.authservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class UserInfoDto {
    private Long id;
    private String email;
    private String firstName;
    private String lastName;
    private String role;
    private String apiKey;
    private LocalDateTime createdAt;
    private boolean enabled;
    private boolean twoFactorEnabled;
}