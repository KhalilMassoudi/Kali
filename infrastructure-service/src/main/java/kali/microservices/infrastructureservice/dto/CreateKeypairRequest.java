package kali.microservices.infrastructureservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreateKeypairRequest {
    @NotNull private Long userId;
    @NotBlank private String name;
    private String publicKey; // omit to have OpenStack generate a keypair server-side
}
