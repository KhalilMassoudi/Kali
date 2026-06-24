package kali.microservices.infrastructureservice.openstack;

import java.util.List;

public interface CloudProvider {
    VpsProvisionResponse createVPS(String os, String ram, String region);
    List<VpsDetails> listVPS(String userId);
    VpsDetails getVPS(String externalId);
    void deleteVPS(String externalId);
    void restartVPS(String externalId);
    void stopVPS(String externalId);
    boolean ping();
}
