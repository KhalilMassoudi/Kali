package kali.microservices.infrastructureservice.health;

import kali.microservices.infrastructureservice.openstack.OpenStackAuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.stereotype.Component;

@Component("openStack")
@RequiredArgsConstructor
public class OpenStackHealthIndicator implements HealthIndicator {

    private final OpenStackAuthService authService;

    @Override
    public Health health() {
        boolean up = authService.ping();
        if (up) {
            return Health.up()
                    .withDetail("region", authService.getRegion())
                    .withDetail("status", "connected")
                    .build();
        }
        return Health.down()
                .withDetail("region", authService.getRegion())
                .withDetail("status", "unreachable")
                .build();
    }
}
