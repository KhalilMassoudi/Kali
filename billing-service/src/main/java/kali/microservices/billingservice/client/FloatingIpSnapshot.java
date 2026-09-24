package kali.microservices.billingservice.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record FloatingIpSnapshot(Long id, Long userId, String floatingIpAddress) {
}
