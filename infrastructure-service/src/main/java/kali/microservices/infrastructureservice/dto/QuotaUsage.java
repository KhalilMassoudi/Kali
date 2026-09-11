package kali.microservices.infrastructureservice.dto;

/**
 * Real, current usage for a user, computed on demand from the owning tables
 * (never persisted) — compared against {@link kali.microservices.infrastructureservice.entities.UserQuota}.
 */
public record QuotaUsage(
        int vmCount,
        int totalVcpu,
        int totalRamMb,
        int volumeCount,
        int totalStorageGb,
        int networkCount,
        int securityGroupCount
) {
}
