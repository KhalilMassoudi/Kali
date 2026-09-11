package kali.microservices.authservice.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class TwoFactorCodeRequest {
    @NotBlank(message = "Le code est obligatoire")
    private String code;
}
