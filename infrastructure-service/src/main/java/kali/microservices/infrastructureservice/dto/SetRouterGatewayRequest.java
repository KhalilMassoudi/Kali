package kali.microservices.infrastructureservice.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class SetRouterGatewayRequest {
    @NotBlank private String externalNetworkId;
}
