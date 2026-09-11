package kali.microservices.infrastructureservice.controller;

import jakarta.validation.Valid;
import kali.microservices.infrastructureservice.dto.AdoptImageRequest;
import kali.microservices.infrastructureservice.dto.ImportImageUrlRequest;
import kali.microservices.infrastructureservice.dto.PatchImageRequest;
import kali.microservices.infrastructureservice.entities.PlatformImage;
import kali.microservices.infrastructureservice.openstack.ImageOption;
import kali.microservices.infrastructureservice.security.AuthContext;
import kali.microservices.infrastructureservice.service.ImageCatalogService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/infrastructure")
@RequiredArgsConstructor
public class ImageCatalogController {

    private final ImageCatalogService imageCatalogService;
    private final AuthContext authContext;

    @GetMapping("/images")
    public ResponseEntity<List<PlatformImage>> listPublished(@RequestHeader("Authorization") String authHeader) {
        authContext.resolve(authHeader);
        return ResponseEntity.ok(imageCatalogService.listPublished());
    }

    @GetMapping("/admin/images")
    public ResponseEntity<List<PlatformImage>> listAll(@RequestHeader("Authorization") String authHeader) {
        authContext.requireAdmin(authHeader);
        return ResponseEntity.ok(imageCatalogService.listAll());
    }

    @GetMapping("/admin/images/available")
    public ResponseEntity<List<ImageOption>> listAvailableForAdoption(@RequestHeader("Authorization") String authHeader) {
        authContext.requireAdmin(authHeader);
        return ResponseEntity.ok(imageCatalogService.listAvailableForAdoption());
    }

    @PostMapping("/admin/images/adopt")
    public ResponseEntity<PlatformImage> adopt(@RequestHeader("Authorization") String authHeader,
                                                 @Valid @RequestBody AdoptImageRequest request) {
        authContext.requireAdmin(authHeader);
        return ResponseEntity.status(HttpStatus.CREATED).body(imageCatalogService.adopt(request));
    }

    @PostMapping("/admin/images/import-url")
    public ResponseEntity<PlatformImage> importFromUrl(@RequestHeader("Authorization") String authHeader,
                                                          @Valid @RequestBody ImportImageUrlRequest request) {
        authContext.requireAdmin(authHeader);
        return ResponseEntity.status(HttpStatus.CREATED).body(imageCatalogService.importFromUrl(request));
    }

    @PatchMapping("/admin/images/{id}")
    public ResponseEntity<PlatformImage> patch(@RequestHeader("Authorization") String authHeader,
                                                 @PathVariable Long id, @RequestBody PatchImageRequest request) {
        authContext.requireAdmin(authHeader);
        return ResponseEntity.ok(imageCatalogService.patch(id, request));
    }

    @DeleteMapping("/admin/images/{id}")
    public ResponseEntity<Void> delete(@RequestHeader("Authorization") String authHeader, @PathVariable Long id) {
        authContext.requireAdmin(authHeader);
        imageCatalogService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
