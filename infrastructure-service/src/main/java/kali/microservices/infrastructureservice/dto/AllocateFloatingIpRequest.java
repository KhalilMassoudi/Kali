package kali.microservices.infrastructureservice.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AllocateFloatingIpRequest {
    @NotNull private Long userId;
    private String pool; // omit to use the first available pool
}
