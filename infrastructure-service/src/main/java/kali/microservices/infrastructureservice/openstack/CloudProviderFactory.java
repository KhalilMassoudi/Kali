package kali.microservices.infrastructureservice.openstack;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Component
public class CloudProviderFactory {

    @Autowired
    @Qualifier("openstack")
    private CloudProvider openStackProvider;

    public CloudProvider getProvider() {
        return openStackProvider;
    }
}