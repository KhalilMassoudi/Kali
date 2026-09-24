package kali.microservices.billingservice.controller;

import jakarta.validation.Valid;
import kali.microservices.billingservice.dto.AdminAdjustRequest;
import kali.microservices.billingservice.entities.PricingConfig;
import kali.microservices.billingservice.entities.Wallet;
import kali.microservices.billingservice.security.AuthContext;
import kali.microservices.billingservice.service.PricingService;
import kali.microservices.billingservice.service.WalletService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/billing/admin")
@RequiredArgsConstructor
public class AdminBillingController {

    private final WalletService walletService;
    private final PricingService pricingService;
    private final AuthContext authContext;

    @GetMapping("/wallets")
    public ResponseEntity<List<Wallet>> getAllWallets(@RequestHeader("Authorization") String authHeader) {
        authContext.requireAdmin(authHeader);
        return ResponseEntity.ok(walletService.getAllWallets());
    }

    @PostMapping("/wallet/user/{userId}/adjust")
    public ResponseEntity<Wallet> adjustWallet(@RequestHeader("Authorization") String authHeader,
                                                @PathVariable Long userId,
                                                @Valid @RequestBody AdminAdjustRequest request) {
        authContext.requireAdmin(authHeader);
        return ResponseEntity.ok(walletService.adminAdjust(userId, request.getAmount(), request.getDescription()));
    }

    @GetMapping("/pricing")
    public ResponseEntity<PricingConfig> getPricing(@RequestHeader("Authorization") String authHeader) {
        authContext.requireAdmin(authHeader);
        return ResponseEntity.ok(pricingService.getConfig());
    }

    @PutMapping("/pricing")
    public ResponseEntity<PricingConfig> updatePricing(@RequestHeader("Authorization") String authHeader,
                                                         @Valid @RequestBody PricingConfig update) {
        authContext.requireAdmin(authHeader);
        return ResponseEntity.ok(pricingService.updateConfig(update));
    }
}
