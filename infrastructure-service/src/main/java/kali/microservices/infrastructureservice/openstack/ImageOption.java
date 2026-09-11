package kali.microservices.infrastructureservice.openstack;

public record ImageOption(String id, String name, Integer minDiskGb, Integer minRamMb) {
}
