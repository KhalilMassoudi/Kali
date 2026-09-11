package kali.microservices.infrastructureservice.repository;

import kali.microservices.infrastructureservice.entities.VpsSecurityGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VpsSecurityGroupRepository extends JpaRepository<VpsSecurityGroup, Long> {
    List<VpsSecurityGroup> findByUserId(Long userId);
    List<VpsSecurityGroup> findByProjectId(Long projectId);

    @Modifying
    @Query("update VpsSecurityGroup g set g.projectId = null where g.projectId = :projectId")
    void clearProjectId(Long projectId);
}
