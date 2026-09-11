package kali.microservices.infrastructureservice.service;

import kali.microservices.infrastructureservice.dto.AdminClientSummary;
import kali.microservices.infrastructureservice.dto.AdminClientSummary.AdminProjectSummary;
import kali.microservices.infrastructureservice.dto.AdminClientSummary.ResourceCounts;
import kali.microservices.infrastructureservice.dto.CreateClientProjectRequest;
import kali.microservices.infrastructureservice.dto.ProjectResourcesSummary;
import kali.microservices.infrastructureservice.dto.UpdateClientProjectRequest;
import kali.microservices.infrastructureservice.entities.ClientProject;
import kali.microservices.infrastructureservice.entities.VpsNetwork;
import kali.microservices.infrastructureservice.entities.VpsSecurityGroup;
import kali.microservices.infrastructureservice.entities.VpsServer;
import kali.microservices.infrastructureservice.entities.VpsVolume;
import kali.microservices.infrastructureservice.repository.ClientProjectRepository;
import kali.microservices.infrastructureservice.repository.VolumeRepository;
import kali.microservices.infrastructureservice.repository.VpsNetworkRepository;
import kali.microservices.infrastructureservice.repository.VpsSecurityGroupRepository;
import kali.microservices.infrastructureservice.repository.VpsServerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * "Projects" here are a pure app-level grouping a client attaches to their own resources —
 * everything still runs inside the single shared OpenStack project (safozi-app). No OpenStack
 * calls happen anywhere in this service. See ClientProject's javadoc for the future-upgrade
 * path to real Keystone projects.
 */
@Service
@RequiredArgsConstructor
public class ClientProjectService {

    private final ClientProjectRepository clientProjectRepository;
    private final VpsServerRepository vpsServerRepository;
    private final VolumeRepository volumeRepository;
    private final VpsNetworkRepository vpsNetworkRepository;
    private final VpsSecurityGroupRepository vpsSecurityGroupRepository;

    public ClientProject createProject(CreateClientProjectRequest request) {
        if (clientProjectRepository.existsByUserIdAndNameIgnoreCase(request.getUserId(), request.getName())) {
            throw new IllegalArgumentException("Vous avez déjà un projet nommé \"" + request.getName() + "\"");
        }
        ClientProject project = new ClientProject();
        project.setUserId(request.getUserId());
        project.setName(request.getName());
        project.setDescription(request.getDescription());
        return clientProjectRepository.save(project);
    }

    public ClientProject renameProject(Long id, UpdateClientProjectRequest request) {
        ClientProject project = getProjectById(id);
        project.setName(request.getName());
        project.setDescription(request.getDescription());
        return clientProjectRepository.save(project);
    }

    public ClientProject getProjectById(Long id) {
        return clientProjectRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Projet introuvable: " + id));
    }

    public List<ClientProject> getProjectsByUser(Long userId) {
        return clientProjectRepository.findByUserId(userId);
    }

    public List<ClientProject> getAllProjects() {
        return clientProjectRepository.findAll();
    }

    /**
     * A resource can only be filed under a project owned by the same client. Pass a null
     * projectId to unassign (always allowed).
     */
    public void validateAssignable(Long projectId, Long resourceOwnerUserId) {
        if (projectId == null) return;
        ClientProject project = getProjectById(projectId);
        if (!project.getUserId().equals(resourceOwnerUserId)) {
            throw new IllegalArgumentException("Ce projet n'appartient pas au propriétaire de cette ressource");
        }
    }

    public ProjectResourcesSummary getProjectResources(Long id) {
        return new ProjectResourcesSummary(
                vpsServerRepository.findByProjectId(id),
                volumeRepository.findByProjectId(id),
                vpsNetworkRepository.findByProjectId(id),
                vpsSecurityGroupRepository.findByProjectId(id)
        );
    }

    /**
     * Per-client rollup for the admin "Clients" page: every user who has at least one project
     * or one resource in infrastructure-service, with their projects' resource counts and an
     * "unassigned" bucket. userId-keyed only — see {@link AdminClientSummary}'s javadoc.
     */
    public List<AdminClientSummary> getAdminClientSummaries() {
        List<ClientProject> allProjects = clientProjectRepository.findAll();
        List<VpsServer> allVps = vpsServerRepository.findAll();
        List<VpsVolume> allVolumes = volumeRepository.findAll();
        List<VpsNetwork> allNetworks = vpsNetworkRepository.findAll();
        List<VpsSecurityGroup> allGroups = vpsSecurityGroupRepository.findAll();

        Set<Long> userIds = new HashSet<>();
        allProjects.forEach(p -> userIds.add(p.getUserId()));
        allVps.forEach(v -> userIds.add(v.getUserId()));
        allVolumes.forEach(v -> userIds.add(v.getUserId()));
        allNetworks.forEach(n -> userIds.add(n.getUserId()));
        allGroups.forEach(g -> userIds.add(g.getUserId()));

        Map<Long, List<ClientProject>> projectsByUser = allProjects.stream()
                .collect(Collectors.groupingBy(ClientProject::getUserId));

        return userIds.stream().map(userId -> {
            List<ClientProject> userProjects = projectsByUser.getOrDefault(userId, List.of());
            List<AdminProjectSummary> projectSummaries = userProjects.stream()
                    .map(p -> new AdminProjectSummary(p.getId(), p.getName(), p.getDescription(), countFor(
                            allVps, allVolumes, allNetworks, allGroups, r -> p.getId().equals(r)
                    )))
                    .toList();

            ResourceCounts unassigned = new ResourceCounts(
                    (int) allVps.stream().filter(v -> v.getUserId().equals(userId) && v.getProjectId() == null).count(),
                    (int) allVolumes.stream().filter(v -> v.getUserId().equals(userId) && v.getProjectId() == null).count(),
                    (int) allNetworks.stream().filter(n -> n.getUserId().equals(userId) && n.getProjectId() == null).count(),
                    (int) allGroups.stream().filter(g -> g.getUserId().equals(userId) && g.getProjectId() == null).count()
            );
            return new AdminClientSummary(userId, projectSummaries, unassigned);
        }).toList();
    }

    private ResourceCounts countFor(List<VpsServer> vps, List<VpsVolume> volumes, List<VpsNetwork> networks,
                                     List<VpsSecurityGroup> groups, Function<Long, Boolean> matchesProject) {
        return new ResourceCounts(
                (int) vps.stream().filter(v -> matchesProject.apply(v.getProjectId())).count(),
                (int) volumes.stream().filter(v -> matchesProject.apply(v.getProjectId())).count(),
                (int) networks.stream().filter(n -> matchesProject.apply(n.getProjectId())).count(),
                (int) groups.stream().filter(g -> matchesProject.apply(g.getProjectId())).count()
        );
    }

    /**
     * A project is a pure label — deleting it never destroys resources, it just orphans them
     * back to projectId=null (the same state every resource is in by default today).
     */
    @Transactional
    public void deleteProject(Long id) {
        vpsServerRepository.clearProjectId(id);
        volumeRepository.clearProjectId(id);
        vpsNetworkRepository.clearProjectId(id);
        vpsSecurityGroupRepository.clearProjectId(id);
        clientProjectRepository.deleteById(id);
    }
}
