package kali.microservices.infrastructureservice.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UpdateQuotaRequest {
    @NotNull @Min(0) private Integer maxVcpu;
    @NotNull @Min(0) private Integer maxRamMb;
    @NotNull @Min(0) private Integer maxStorageGb;
    @NotNull @Min(0) private Integer maxVms;
    @NotNull @Min(0) private Integer maxVolumes;
    @NotNull @Min(0) private Integer maxNetworks;
    @NotNull @Min(0) private Integer maxSecurityGroups;
}
