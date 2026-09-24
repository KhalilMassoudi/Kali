package kali.microservices.billingservice.controller;

import jakarta.validation.Valid;
import kali.microservices.billingservice.dto.RechargeRequest;
import kali.microservices.billingservice.entities.Wallet;
import kali.microservices.billingservice.entities.WalletTransaction;
import kali.microservices.billingservice.security.AuthContext;
import kali.microservices.billingservice.service.WalletService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/billing/wallet")
@RequiredArgsConstructor
public class WalletController {

    private final WalletService walletService;
    private final AuthContext authContext;

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
