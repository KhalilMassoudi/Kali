package kali.microservices.infrastructureservice.service;

import kali.microservices.infrastructureservice.dto.AdoptImageRequest;
import kali.microservices.infrastructureservice.dto.ImportImageUrlRequest;
import kali.microservices.infrastructureservice.dto.PatchImageRequest;
import kali.microservices.infrastructureservice.entities.PlatformImage;
import kali.microservices.infrastructureservice.openstack.CloudProviderFactory;
import kali.microservices.infrastructureservice.openstack.ImageOption;
import kali.microservices.infrastructureservice.repository.PlatformImageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ImageCatalogService {

    private final PlatformImageRepository repository;
    private final CloudProviderFactory cloudProviderFactory;

    public List<ImageOption> listAvailableForAdoption() {
        List<String> alreadyAdopted = repository.findAll().stream().map(PlatformImage::getExternalId).toList();
        return cloudProviderFactory.getProvider().listRawImages().stream()
                .filter(img -> !alreadyAdopted.contains(img.id()))
                .toList();
    }

    public PlatformImage adopt(AdoptImageRequest request) {
        PlatformImage image = new PlatformImage();
        image.setExternalId(request.getExternalId());
        image.setDisplayName(request.getDisplayName());
        image.setOsDistro(request.getOsDistro());
        image.setOsVersion(request.getOsVersion());
        image.setSource(PlatformImage.ImageSource.ADOPTED);
        image.setVisibility(PlatformImage.ImageVisibility.HIDDEN);
        return repository.save(image);
    }

    public PlatformImage importFromUrl(ImportImageUrlRequest request) {
        String externalId = cloudProviderFactory.getProvider().importImageFromUrl(
                request.getDisplayName(), request.getImageUrl(), request.getDiskFormat(),
                request.getMinDiskGb(), request.getMinRamMb());

        PlatformImage image = new PlatformImage();
        image.setExternalId(externalId);
        image.setDisplayName(request.getDisplayName());
        image.setOsDistro(request.getOsDistro());
        image.setOsVersion(request.getOsVersion());
        image.setMinDiskGb(request.getMinDiskGb());
        image.setMinRamMb(request.getMinRamMb());
        image.setSource(PlatformImage.ImageSource.IMPORTED_URL);
        image.setVisibility(PlatformImage.ImageVisibility.HIDDEN);
        return repository.save(image);
    }

    public List<PlatformImage> listAll() {
        return repository.findAll();
    }

    public List<PlatformImage> listPublished() {
        return repository.findByVisibility(PlatformImage.ImageVisibility.PUBLISHED);
    }

    public PlatformImage patch(Long id, PatchImageRequest request) {
        PlatformImage image = getById(id);
        if (request.getDisplayName() != null) image.setDisplayName(request.getDisplayName());
        if (request.getVisibility() != null) image.setVisibility(request.getVisibility());
        return repository.save(image);
    }

    public void delete(Long id) {
        PlatformImage image = getById(id);
        if (image.getSource() != PlatformImage.ImageSource.ADOPTED) {
            cloudProviderFactory.getProvider().deleteImage(image.getExternalId());
        }
        repository.delete(image);
    }

    private PlatformImage getById(Long id) {
        return repository.findById(id).orElseThrow(() -> new RuntimeException("Image not found: " + id));
    }
}
