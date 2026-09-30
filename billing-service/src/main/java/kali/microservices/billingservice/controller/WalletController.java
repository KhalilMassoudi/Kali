package kali.microservices.billingservice.controller;

import jakarta.validation.Valid;
import kali.microservices.billingservice.dto.RechargeRequest;
import kali.microservices.billingservice.dto.UsageSummary;
import kali.microservices.billingservice.dto.VmUsage;
import kali.microservices.billingservice.entities.PricingConfig;
import kali.microservices.billingservice.entities.Wallet;
import kali.microservices.billingservice.entities.WalletTransaction;
import kali.microservices.billingservice.security.AuthContext;
import kali.microservices.billingservice.service.PricingService;
import kali.microservices.billingservice.service.UsageSummaryService;
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
    private final UsageSummaryService usageSummaryService;
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

    /** Spend over the last `days` days (7 or 30), per day and per resource type. */
    @GetMapping("/user/{userId}/usage")
    public ResponseEntity<UsageSummary> getUsage(@RequestHeader("Authorization") String authHeader,
                                                 @PathVariable Long userId,
                                                 @RequestParam(defaultValue = "30") int days) {
        authContext.requireOwnerOrAdmin(authHeader, userId);
        return ResponseEntity.ok(usageSummaryService.summarize(userId, days == 7 ? 7 : 30));
    }

    /** One VM's charges, tick by tick, with the wallet balance around each deduction. */
    @GetMapping("/user/{userId}/usage/vm/{vmId}")
    public ResponseEntity<VmUsage> getVmUsage(@RequestHeader("Authorization") String authHeader,
                                              @PathVariable Long userId, @PathVariable Long vmId) {
        authContext.requireOwnerOrAdmin(authHeader, userId);
        return ResponseEntity.ok(usageSummaryService.vmUsage(userId, vmId));
    }

    @PostMapping("/user/{userId}/recharge")
    public ResponseEntity<Wallet> recharge(@RequestHeader("Authorization") String authHeader,
                                            @PathVariable Long userId,
                                            @Valid @RequestBody RechargeRequest request) {
        authContext.requireOwnerOrAdmin(authHeader, userId);
        return ResponseEntity.ok(walletService.recharge(userId, request.getAmount(), "Recharge de crédit"));
    }
}
