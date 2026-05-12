package kali.microservices.infrastructureservice.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreateClusterRequest {

    @NotNull
    private Long userId;

    @NotBlank
    private String name;

    private String kubernetesVersion; // ex: 1.28

    @NotNull
    @Min(1)
    private Integer nodeCount;

    @Min(1)
    private Integer cpuPerNode;

    @Min(1024)
    private Integer ramPerNode;   // en MB

    private String region;
}