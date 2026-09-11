package kali.microservices.infrastructureservice.dto;

import kali.microservices.infrastructureservice.entities.PlatformImage;
import lombok.Data;

@Data
public class PatchImageRequest {
    private String displayName;
    private PlatformImage.ImageVisibility visibility;
}
