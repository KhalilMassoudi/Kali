package kali.microservices.infrastructureservice.service;

import kali.microservices.infrastructureservice.dto.CreateSecurityGroupRequest;
import kali.microservices.infrastructureservice.entities.VpsSecurityGroup;
import kali.microservices.infrastructureservice.openstack.CloudProviderFactory;
import kali.microservices.infrastructureservice.openstack.SecurityGroupRuleOption;
import kali.microservices.infrastructureservice.repository.VpsSecurityGroupRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class SecurityGroupManagementService {

    private final VpsSecurityGroupRepository repository;
    private final CloudProviderFactory cloudProviderFactory;
    private final QuotaService quotaService;
    private final ClientProjectService clientProjectService;

    public VpsSecurityGroup assignProject(Long id, Long projectId) {
        VpsSecurityGroup group = getById(id);
        clientProjectService.validateAssignable(projectId, group.getUserId());
        group.setProjectId(projectId);
        return repository.save(group);
    }

    public VpsSecurityGroup create(CreateSecurityGroupRequest request) {
        quotaService.checkSecurityGroupCreation(request.getUserId());

        VpsSecurityGroup group = new VpsSecurityGroup();
        group.setUserId(request.getUserId());
        group.setName(request.getName());
        group.setDescription(request.getDescription());
        group.setStatus(VpsSecurityGroup.SecurityGroupStatus.ACTIVE);

        try {
            String externalId = cloudProviderFactory.getProvider()
                    .createSecurityGroup(request.getName(), request.getDescription());
            group.setExternalId(externalId);
        } catch (Exception e) {
            log.error("OpenStack security group creation failed: {}", e.getMessage());
            group.setStatus(VpsSecurityGroup.SecurityGroupStatus.ERROR);
        }
        return repository.save(group);
    }

    public List<VpsSecurityGroup> getByUser(Long userId) {
        return repository.findByUserId(userId).stream()
                .filter(g -> g.getStatus() != VpsSecurityGroup.SecurityGroupStatus.DELETED)
                .toList();
    }

    public List<VpsSecurityGroup> getAll() {
        return repository.findAll().stream()
                .filter(g -> g.getStatus() != VpsSecurityGroup.SecurityGroupStatus.DELETED)
                .toList();
    }

    public VpsSecurityGroup getById(Long id) {
        return repository.findById(id).orElseThrow(() -> new RuntimeException("Security group not found: " + id));
    }

    public void delete(Long id) {
        VpsSecurityGroup group = getById(id);
        if (group.getExternalId() != null) {
            try {
                cloudProviderFactory.getProvider().deleteSecurityGroup(group.getExternalId());
                group.setStatus(VpsSecurityGroup.SecurityGroupStatus.DELETED);
            } catch (Exception e) {
                log.error("Failed to delete OpenStack security group {}: {}", group.getExternalId(), e.getMessage());
                group.setStatus(VpsSecurityGroup.SecurityGroupStatus.ERROR);
                repository.save(group);
                throw new RuntimeException("Failed to delete security group on OpenStack: " + e.getMessage(), e);
            }
        } else {
            group.setStatus(VpsSecurityGroup.SecurityGroupStatus.DELETED);
        }
        repository.save(group);
    }

    public List<SecurityGroupRuleOption> listRules(Long id) {
        VpsSecurityGroup group = getById(id);
        if (group.getExternalId() == null) return List.of();
        return cloudProviderFactory.getProvider().listSecurityGroupRules(group.getExternalId());
    }

    public SecurityGroupRuleOption addRule(Long id, String direction, String protocol,
                                            Integer portMin, Integer portMax, String cidr) {
        VpsSecurityGroup group = getById(id);
        if (group.getExternalId() == null) {
            throw new RuntimeException("Security group " + id + " has no OpenStack group associated with it");
        }
        return cloudProviderFactory.getProvider()
                .addSecurityGroupRule(group.getExternalId(), direction, protocol, portMin, portMax, cidr);
    }

    public void removeRule(Long id, String ruleId) {
        getById(id); // ensures existence/ownership was already checked by the controller
        cloudProviderFactory.getProvider().removeSecurityGroupRule(ruleId);
    }
}
