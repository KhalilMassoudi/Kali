package kali.microservices.infrastructureservice.dto;

import java.util.List;

/**
 * Per-client rollup for the admin "Clients" page. userId-keyed only — email/name/role live in
 * auth-service, a different microservice; the frontend merges this with its already-loaded
 * admin user list by userId rather than infrastructure-service calling back into auth-service.
 */
public record AdminClientSummary(
        Long userId,
        List<AdminProjectSummary> projects,
        ResourceCounts unassigned
) {
    public record AdminProjectSummary(Long id, String name, String description, ResourceCounts counts) {
    }

    public record ResourceCounts(int vps, int volumes, int networks, int securityGroups) {
    }
}
