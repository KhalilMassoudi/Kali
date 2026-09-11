package kali.microservices.authservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class AdminUserDto {
    private Long id;
    private String email;
    private String firstName;
    private String lastName;
    private String role;
    private boolean enabled;
    private LocalDateTime createdAt;
}
