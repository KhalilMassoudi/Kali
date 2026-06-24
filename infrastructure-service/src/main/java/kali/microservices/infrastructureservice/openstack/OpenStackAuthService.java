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
            return client;
        }
        // Re-authenticate if token is expired or about to expire (within 60s)
        Date expires = client.getToken().getExpires();
        if (expires != null && expires.before(new Date(System.currentTimeMillis() + 60_000))) {
            log.info("OpenStack token expired or expiring soon, re-authenticating...");
            authenticate();
        }
        return client;
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
}