package kali.microservices.infrastructureservice.repository;

import kali.microservices.infrastructureservice.entities.VpsVolume;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VolumeRepository extends JpaRepository<VpsVolume, Long> {
    List<VpsVolume> findByUserId(Long userId);
    List<VpsVolume> findByAttachedVpsId(Long attachedVpsId);
    List<VpsVolume> findByProjectId(Long projectId);

    @Modifying
    @Query("update VpsVolume v set v.projectId = null where v.projectId = :projectId")
    void clearProjectId(Long projectId);
}
