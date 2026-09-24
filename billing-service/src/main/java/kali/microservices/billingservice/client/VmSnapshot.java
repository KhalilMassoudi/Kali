package kali.microservices.billingservice.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/** Mirrors just the fields the metering job needs from infrastructure-service's VpsServer. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record VmSnapshot(Long id, Long userId, Integer ram, Integer cpu, Integer storage, String status) {
    public boolean isRunning() {
        return "RUNNING".equals(status);
    }

    public boolean isBillableForStorage() {
        return status != null && !status.equals("DELETED") && !status.equals("ERROR");
    }
}
