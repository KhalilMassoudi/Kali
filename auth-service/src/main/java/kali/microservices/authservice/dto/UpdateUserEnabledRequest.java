package kali.microservices.authservice.dto;

import lombok.Data;

@Data
public class UpdateUserEnabledRequest {
    private boolean enabled;
}
