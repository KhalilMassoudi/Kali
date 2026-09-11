package kali.microservices.infrastructureservice.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreateVolumeRequest {

    @NotNull
    private Long userId;

    @NotBlank
    private String name;

    @NotNull
    @Min(1)
    private Integer sizeGb;

    private String region;
}
