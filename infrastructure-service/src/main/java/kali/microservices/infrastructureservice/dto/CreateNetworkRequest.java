package kali.microservices.infrastructureservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreateNetworkRequest {
    @NotNull private Long userId;
    @NotBlank private String name;
    @NotBlank private String cidr;
}
