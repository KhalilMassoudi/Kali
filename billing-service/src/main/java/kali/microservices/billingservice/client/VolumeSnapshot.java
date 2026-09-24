package kali.microservices.billingservice.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record VolumeSnapshot(Long id, Long userId, Integer sizeGb, String status) {
    public boolean isBillable() {
        return status != null && !status.equals("DELETED") && !status.equals("ERROR");
    }
}
