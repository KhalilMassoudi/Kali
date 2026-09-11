package kali.microservices.infrastructureservice.dto;

import kali.microservices.infrastructureservice.entities.VpsNetwork;
import kali.microservices.infrastructureservice.entities.VpsSecurityGroup;
import kali.microservices.infrastructureservice.entities.VpsServer;
import kali.microservices.infrastructureservice.entities.VpsVolume;

import java.util.List;

public record ProjectResourcesSummary(
        List<VpsServer> vps,
        List<VpsVolume> volumes,
        List<VpsNetwork> networks,
        List<VpsSecurityGroup> securityGroups
) {
}
