package kali.microservices.infrastructureservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreateVolumeSnapshotRequest {
    @NotNull private Long userId;
    @NotNull private Long volumeId; // VpsVolume.id (app-level, not the Cinder UUID)
    @NotBlank private String name;
    private String description;
}
