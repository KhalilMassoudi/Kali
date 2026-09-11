package kali.microservices.infrastructureservice.dto;

import java.time.LocalDateTime;
import java.util.Map;

public record VmHealthResponse(
        String status,
        String ipAddress,
        String floatingIp,
        Map<String, Double> diagnostics,
        LocalDateTime checkedAt
) {
}
