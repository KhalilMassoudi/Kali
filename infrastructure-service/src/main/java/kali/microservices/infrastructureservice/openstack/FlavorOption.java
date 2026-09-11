package kali.microservices.infrastructureservice.openstack;

public record FlavorOption(String id, String name, int ram, int vcpus, int disk) {
}
