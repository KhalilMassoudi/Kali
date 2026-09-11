package kali.microservices.authservice.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UpdateSmtpConfigRequest {
    @NotBlank
    private String host;

    @NotNull
    private Integer port;

    private String username;

    /** Left blank/null to keep the currently stored password unchanged. */
    private String password;

    @NotBlank
    @Email
    private String fromAddress;

    private String fromName;

    private boolean authEnabled = true;

    @NotBlank
    private String encryption;
}
