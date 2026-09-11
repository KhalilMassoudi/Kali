package kali.microservices.infrastructureservice.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class RouterInterfaceRequest {
    @NotBlank private String subnetId;
}
