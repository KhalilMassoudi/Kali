package kali.microservices.authservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class NotifyUserRequest {

    @NotNull
    private Long userId;

    @NotBlank
    private String subject;

    @NotBlank
    private String body;
}
