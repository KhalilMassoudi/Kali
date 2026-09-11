package kali.microservices.infrastructureservice.openstack;

public record InterfaceDetails(String attachmentId, String portId, String networkId, String macAddress, String fixedIpAddress) {
}
