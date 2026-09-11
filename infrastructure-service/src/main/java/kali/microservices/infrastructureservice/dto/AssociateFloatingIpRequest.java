package kali.microservices.infrastructureservice.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AssociateFloatingIpRequest {
    @NotNull private Long vpsId;
}
