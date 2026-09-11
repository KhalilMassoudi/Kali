package kali.microservices.infrastructureservice.repository;

import kali.microservices.infrastructureservice.entities.PlatformImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PlatformImageRepository extends JpaRepository<PlatformImage, Long> {
    List<PlatformImage> findByVisibility(PlatformImage.ImageVisibility visibility);
    Optional<PlatformImage> findByExternalId(String externalId);
}
