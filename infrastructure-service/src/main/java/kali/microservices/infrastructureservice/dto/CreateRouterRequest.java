package kali.microservices.infrastructureservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreateRouterRequest {
    @NotNull private Long userId;
    @NotBlank private String name;
    private String externalNetworkId; // optional — sets the gateway at creation time
}
