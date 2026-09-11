package kali.microservices.infrastructureservice.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UpdateClientProjectRequest {
    @NotBlank private String name;
    private String description;
}
