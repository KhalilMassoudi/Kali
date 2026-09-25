package kali.microservices.billingservice.controller;

import jakarta.validation.Valid;
import kali.microservices.billingservice.dto.RechargeRequest;
import kali.microservices.billingservice.entities.PricingConfig;
import kali.microservices.billingservice.entities.Wallet;
import kali.microservices.billingservice.entities.WalletTransaction;
import kali.microservices.billingservice.security.AuthContext;
import kali.microservices.billingservice.service.PricingService;
import kali.microservices.billingservice.service.WalletService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/billing/wallet")
@RequiredArgsConstructor
public class WalletController {

    private final WalletService walletService;
    private final PricingService pricingService;
    private final AuthContext authContext;

    /** The admin-set low-balance alert level, for the client billing page's warning banner. */
    @GetMapping("/alert-threshold")
    public ResponseEntity<Map<String, Object>> getAlertThreshold(@RequestHeader("Authorization") String authHeader) {
        authContext.resolve(authHeader);
        PricingConfig config = pricingService.getConfig();
        return ResponseEntity.ok(Map.of("threshold", config.getLowBalanceThreshold(), "currency",
                config.getCurrency() != null ? config.getCurrency() : "TND"));
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<Wallet> getWallet(@RequestHeader("Authorization") String authHeader, @PathVariable Long userId) {
        authContext.requireOwnerOrAdmin(authHeader, userId);
        return ResponseEntity.ok(walletService.getOrCreateWallet(userId));
    }

    @GetMapping("/user/{userId}/transactions")
    public ResponseEntity<List<WalletTransaction>> getTransactions(@RequestHeader("Authorization") String authHeader,
                                                                     @PathVariable Long userId) {
        authContext.requireOwnerOrAdmin(authHeader, userId);
        return ResponseEntity.ok(walletService.getTransactions(userId));
    }

    @PostMapping("/user/{userId}/recharge")
    public ResponseEntity<Wallet> recharge(@RequestHeader("Authorization") String authHeader,
                                            @PathVariable Long userId,
                                            @Valid @RequestBody RechargeRequest request) {
        authContext.requireOwnerOrAdmin(authHeader, userId);
        return ResponseEntity.ok(walletService.recharge(userId, request.getAmount(), "Recharge de crédit"));
    }
}
