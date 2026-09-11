package kali.microservices.infrastructureservice.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AdoptImageRequest {
    @NotBlank private String externalId;
    @NotBlank private String displayName;
    private String osDistro;
    private String osVersion;
}
