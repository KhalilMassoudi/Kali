package kali.microservices.infrastructureservice.controller;

import jakarta.validation.Valid;
import kali.microservices.infrastructureservice.dto.QuotaResponse;
import kali.microservices.infrastructureservice.dto.UpdateQuotaRequest;
import kali.microservices.infrastructureservice.entities.UserQuota;
import kali.microservices.infrastructureservice.security.AuthContext;
import kali.microservices.infrastructureservice.security.AuthenticatedUser;
import kali.microservices.infrastructureservice.service.QuotaService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/infrastructure")
@RequiredArgsConstructor
public class QuotaController {

    private final QuotaService quotaService;
    private final AuthContext authContext;

    @GetMapping("/quota/me")
    public ResponseEntity<QuotaResponse> getMyQuota(@RequestHeader("Authorization") String authHeader) {
        AuthenticatedUser user = authContext.resolve(authHeader);
        return ResponseEntity.ok(new QuotaResponse(
                quotaService.getOrCreateDefault(user.userId()), quotaService.getUsage(user.userId())));
    }

    @GetMapping("/admin/quotas/{userId}")
    public ResponseEntity<QuotaResponse> getQuota(@RequestHeader("Authorization") String authHeader,
                                                    @PathVariable Long userId) {
        authContext.requireAdmin(authHeader);
        return ResponseEntity.ok(new QuotaResponse(
                quotaService.getOrCreateDefault(userId), quotaService.getUsage(userId)));
    }

    @PutMapping("/admin/quotas/{userId}")
    public ResponseEntity<UserQuota> updateQuota(@RequestHeader("Authorization") String authHeader,
                                                   @PathVariable Long userId,
                                                   @Valid @RequestBody UpdateQuotaRequest request) {
        authContext.requireAdmin(authHeader);
        UserQuota patch = new UserQuota();
        patch.setMaxVcpu(request.getMaxVcpu());
        patch.setMaxRamMb(request.getMaxRamMb());
        patch.setMaxStorageGb(request.getMaxStorageGb());
        patch.setMaxVms(request.getMaxVms());
        patch.setMaxVolumes(request.getMaxVolumes());
        patch.setMaxNetworks(request.getMaxNetworks());
        patch.setMaxSecurityGroups(request.getMaxSecurityGroups());
        return ResponseEntity.ok(quotaService.updateQuota(userId, patch));
    }
}
