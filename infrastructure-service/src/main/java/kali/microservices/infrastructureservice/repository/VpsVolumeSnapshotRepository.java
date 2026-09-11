package kali.microservices.infrastructureservice.repository;

import kali.microservices.infrastructureservice.entities.VpsVolumeSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VpsVolumeSnapshotRepository extends JpaRepository<VpsVolumeSnapshot, Long> {
    List<VpsVolumeSnapshot> findByUserId(Long userId);
    List<VpsVolumeSnapshot> findBySourceVolumeId(Long sourceVolumeId);
}
