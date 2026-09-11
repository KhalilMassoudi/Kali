package kali.microservices.infrastructureservice.dto;

import kali.microservices.infrastructureservice.entities.UserQuota;

public record QuotaResponse(UserQuota quota, QuotaUsage usage) {
}
