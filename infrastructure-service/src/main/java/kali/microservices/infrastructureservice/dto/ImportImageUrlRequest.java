package kali.microservices.infrastructureservice.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ImportImageUrlRequest {
    @NotBlank private String displayName;
    private String osDistro;
    private String osVersion;
    @NotBlank private String imageUrl;
    private String diskFormat = "qcow2";
    private Integer minDiskGb;
    private Integer minRamMb;
}
