package kali.microservices.infrastructureservice.openstack;

public record FloatingIpDetails(String address, String pool, String instanceExternalId, String fixedIpAddress) {
}
