package kali.microservices.infrastructureservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreateDomainRequest {

    @NotNull
    private Long userId;

    @NotBlank
    private String name;   // ex: monsite.com

    private String nameserver1;
    private String nameserver2;
}