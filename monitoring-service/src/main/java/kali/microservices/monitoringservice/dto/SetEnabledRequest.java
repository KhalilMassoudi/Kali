package kali.microservices.monitoringservice.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class SetEnabledRequest {
    @NotNull
    private Boolean enabled;
}
