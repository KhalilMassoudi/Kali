package kali.microservices.infrastructureservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreateSecurityGroupRequest {
    @NotNull private Long userId;
    @NotBlank private String name;
    private String description;
}
