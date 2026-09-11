package kali.microservices.authservice.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateProfileRequest {
    @Size(max = 100, message = "Le prénom ne doit pas dépasser 100 caractères")
    private String firstName;

    @Size(max = 100, message = "Le nom ne doit pas dépasser 100 caractères")
    private String lastName;
}
