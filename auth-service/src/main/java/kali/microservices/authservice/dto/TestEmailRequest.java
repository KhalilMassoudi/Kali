package kali.microservices.authservice.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class TestEmailRequest {
    @NotBlank
    @Email
    private String recipient;
}
