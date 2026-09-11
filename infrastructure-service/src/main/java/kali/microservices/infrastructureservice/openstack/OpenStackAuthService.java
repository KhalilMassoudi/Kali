package kali.microservices.infrastructureservice.openstack;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.openstack4j.api.OSClient;
import org.openstack4j.model.common.Identifier;
import org.openstack4j.openstack.OSFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Date;

@Slf4j
@Service
public class OpenStackAuthService {

    @Value("${openstack.auth.endpoint}")
    private String endpoint;

    @Value("${openstack.auth.username}")
    private String username;

    @Value("${openstack.auth.password}")
    private String password;

    @Value("${openstack.auth.project}")
    private String project;

    @Value("${openstack.auth.domain}")
    private String domain;

    @Value("${openstack.auth.region}")
    private String region;

    private OSClient.OSClientV3 client;

    @PostConstruct
    public void init() {
        try {
            authenticate();
        } catch (Exception e) {
            log.warn("OpenStack initial authentication failed: {}. Will retry on first use.", e.getMessage());
        }
    }

    public synchronized void authenticate() {
        log.info("Authenticating with OpenStack Keystone at {}", endpoint);
        client = OSFactory.builderV3()
                .endpoint(endpoint)
                .credentials(username, password, Identifier.byName(domain))
                .scopeToProject(Identifier.byName(project), Identifier.byName(domain))
                .authenticate();
        String tokenPreview = client.getToken().getId();
        log.info("OpenStack authentication successful. Token: {}...", tokenPreview.substring(0, Math.min(10, tokenPreview.length())));
    }

    public synchronized OSClient.OSClientV3 getClient() {
        if (client == null) {
            authenticate();
        } else {
            // Re-authenticate if token is expired or about to expire (within 60s)
            Date expires = client.getToken().getExpires();
            if (expires != null && expires.before(new Date(System.currentTimeMillis() + 60_000))) {
                log.info("OpenStack token expired or expiring soon, re-authenticating...");
                authenticate();
            }
        }
        // openstack4j binds a client to a ThreadLocal session set on whichever thread called
        // authenticate() (here, the @PostConstruct/init thread) — every other thread (every
        // Tomcat request worker) has no session and throws "Unable to retrieve current
        // session" the moment it calls any service method. clientFromToken() cheaply re-binds
        // the already-obtained token to the CURRENT (calling) thread — no network round-trip.
        return OSFactory.clientFromToken(client.getToken());
    }

    public boolean ping() {
        try {
            getClient();
            return true;
        } catch (Exception e) {
            log.error("OpenStack ping failed: {}", e.getMessage());
            return false;
        }
    }

    public String getRegion() {
        return region;
    }

    /**
     * Raw token id, for OpenStack APIs openstack4j has no client for (e.g. Cinder's Generic
     * Volume Groups extension — see CinderGroupClient). Rides the same safozi-app project
     * token as everything else; no new credentials.
     */
    public String getRawToken() {
        return getClient().getToken().getId();
    }

    /**
     * The volumev3 (Cinder) service's public endpoint URL for this token's project, region-
     * filtered. Read from the token's own service catalog — no extra network round-trip.
     */
    public String getCinderEndpoint() {
        return getClient().getToken().getCatalog().stream()
                .filter(service -> "volumev3".equals(service.getType()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Aucun service volumev3 (Cinder) dans le catalogue OpenStack"))
                .getEndpoints().stream()
                .filter(endpoint -> endpoint.getIface() == org.openstack4j.api.types.Facing.PUBLIC)
                .filter(endpoint -> region == null || region.equals(endpoint.getRegion()))
                .findFirst()
                .map(endpoint -> endpoint.getUrl().toString())
                .orElseThrow(() -> new IllegalStateException("Aucun endpoint public volumev3 pour la région " + region));
    }
}