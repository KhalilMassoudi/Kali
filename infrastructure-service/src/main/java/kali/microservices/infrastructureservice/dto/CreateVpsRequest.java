package kali.microservices.infrastructureservice.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

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

    private String networkId;          // OpenStack network UUID; auto-picked if omitted
    private List<String> securityGroups; // security group names to assign after boot

    /**
     * Explicit Glance image id — set when the client picks a catalog image (Phase 9) or
     * redeploys from a personal snapshot/backup (Phase 6). When present, this takes priority
     * over resolving `os` by name.
     */
    private String imageId;

    /** Client-facing ClientKeypair.id (not the raw OpenStack name) — resolved + ownership-checked in VpsService. */
    private Long keypairId;

    /** Client-facing ClientServerGroup.id (not the raw OpenStack UUID) — resolved + ownership-checked in VpsService. */
    private Long serverGroupId;
}