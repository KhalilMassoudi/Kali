package kali.microservices.authservice.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class TwoFactorLoginVerifyRequest {
    @NotBlank(message = "Session invalide")
    private String pendingToken;

    @NotBlank(message = "Le code est obligatoire")
    private String code;
}
