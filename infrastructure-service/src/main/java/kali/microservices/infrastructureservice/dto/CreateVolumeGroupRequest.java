package kali.microservices.infrastructureservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class CreateVolumeGroupRequest {
    @NotNull private Long userId;
    @NotBlank private String name;
    private String description;
    @NotBlank private String groupTypeId;
    @NotEmpty private List<String> volumeTypeIds;
}
