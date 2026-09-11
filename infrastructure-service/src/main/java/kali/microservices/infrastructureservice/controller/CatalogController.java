package kali.microservices.infrastructureservice.controller;

import kali.microservices.infrastructureservice.openstack.CloudProviderFactory;
import kali.microservices.infrastructureservice.openstack.FlavorOption;
import kali.microservices.infrastructureservice.openstack.NetworkOption;
import kali.microservices.infrastructureservice.openstack.SecurityGroupOption;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/infrastructure/catalog")
@RequiredArgsConstructor
public class CatalogController {

    private final CloudProviderFactory cloudProviderFactory;

    @GetMapping("/networks")
    public ResponseEntity<List<NetworkOption>> listNetworks() {
        return ResponseEntity.ok(cloudProviderFactory.getProvider().listNetworks());
    }

    @GetMapping("/security-groups")
    public ResponseEntity<List<SecurityGroupOption>> listSecurityGroups() {
        return ResponseEntity.ok(cloudProviderFactory.getProvider().listSecurityGroups());
    }

    @GetMapping("/floating-ip-pools")
    public ResponseEntity<List<String>> listFloatingIpPools() {
        return ResponseEntity.ok(cloudProviderFactory.getProvider().listFloatingIpPools());
    }

    @GetMapping("/flavors")
    public ResponseEntity<List<FlavorOption>> listFlavors() {
        return ResponseEntity.ok(cloudProviderFactory.getProvider().listFlavors());
    }
}
