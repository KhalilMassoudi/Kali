package kali.microservices.authservice.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class DisableTwoFactorRequest {
    @NotBlank(message = "Le mot de passe est obligatoire")
    private String password;

    @NotBlank(message = "Le code est obligatoire")
    private String code;
}
