package kali.microservices.monitoringservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ApiLogIngestRequest {

    @NotBlank
    private String method;

    @NotBlank
    private String path;

    private String targetService;
    private String userEmail;
    private String userRole;

    @NotNull
    private Integer statusCode;

    @NotNull
    private Long durationMs;
}
