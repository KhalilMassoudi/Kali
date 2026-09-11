package kali.microservices.infrastructureservice.openstack;

public record SnapshotDetails(String id, String name, String volumeId, String status, int sizeGb) {
}
