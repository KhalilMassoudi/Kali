package kali.microservices.infrastructureservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import kali.microservices.infrastructureservice.entities.ClientServerGroup;
import lombok.Data;

@Data
public class CreateServerGroupRequest {
    @NotNull private Long userId;
    @NotBlank private String name;
    @NotNull private ClientServerGroup.Policy policy;
}
