package kali.microservices.infrastructureservice.openstack;

public record RouterDetails(String id, String name, String externalGatewayNetworkId, boolean adminStateUp) {
}
