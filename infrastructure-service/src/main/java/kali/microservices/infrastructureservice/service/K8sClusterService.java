package kali.microservices.infrastructureservice.service;

import kali.microservices.infrastructureservice.dto.CreateClusterRequest;
import kali.microservices.infrastructureservice.entities.K8sCluster;
import kali.microservices.infrastructureservice.repository.K8sClusterRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class K8sClusterService {

    private final K8sClusterRepository k8sClusterRepository;

    public K8sCluster createCluster(CreateClusterRequest request) {
        K8sCluster cluster = new K8sCluster();
        cluster.setUserId(request.getUserId());
        cluster.setName(request.getName());
        cluster.setKubernetesVersion(request.getKubernetesVersion() != null ? request.getKubernetesVersion() : "1.28");
        cluster.setNodeCount(request.getNodeCount());
        cluster.setCpuPerNode(request.getCpuPerNode() != null ? request.getCpuPerNode() : 2);
        cluster.setRamPerNode(request.getRamPerNode() != null ? request.getRamPerNode() : 4096);
        cluster.setRegion(request.getRegion() != null ? request.getRegion() : "eu-west-1");
        cluster.setStatus(K8sCluster.ClusterStatus.RUNNING);
        cluster.setApiEndpoint("https://k8s-" + request.getName().toLowerCase() + ".kali-cloud.tn:6443");

        return k8sClusterRepository.save(cluster);
    }

    public List<K8sCluster> getClustersByUser(Long userId) {
        return k8sClusterRepository.findByUserId(userId);
    }

    public K8sCluster getClusterById(Long id) {
        return k8sClusterRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Cluster K8s non trouvé: " + id));
    }

    public K8sCluster scaleCluster(Long id, int nodeCount) {
        K8sCluster cluster = getClusterById(id);
        cluster.setNodeCount(nodeCount);
        cluster.setStatus(K8sCluster.ClusterStatus.RUNNING);
        return k8sClusterRepository.save(cluster);
    }

    public void deleteCluster(Long id) {
        K8sCluster cluster = getClusterById(id);
        cluster.setStatus(K8sCluster.ClusterStatus.DELETED);
        k8sClusterRepository.save(cluster);
    }
}