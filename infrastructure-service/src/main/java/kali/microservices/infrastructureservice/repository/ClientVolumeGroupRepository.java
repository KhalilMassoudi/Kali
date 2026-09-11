package kali.microservices.infrastructureservice.repository;

import kali.microservices.infrastructureservice.entities.ClientVolumeGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ClientVolumeGroupRepository extends JpaRepository<ClientVolumeGroup, Long> {
    List<ClientVolumeGroup> findByUserId(Long userId);
}
