package kali.microservices.infrastructureservice.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreateVpsRequest {

    @NotNull
    private Long userId;

    @NotBlank
    private String name;

    @NotBlank
    private String os;         // ubuntu, debian, centos

    @NotNull
    @Min(512)
    private Integer ram;       // en MB

    @NotNull
    @Min(1)
    private Integer cpu;       // nombre de vCPU

    @NotNull
    @Min(10)
    private Integer storage;   // en GB

    private String region;     // ex: eu-west-1
}