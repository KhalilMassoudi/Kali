package kali.microservices.authservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ChangePasswordRequest {
    @NotBlank(message = "Mot de passe actuel est obligatoire")
    private String currentPassword;

    @NotBlank(message = "Nouveau mot de passe est obligatoire")
    @Size(min = 6, message = "Nouveau mot de passe doit contenir au moins 6 caractères")
    private String newPassword;
}
