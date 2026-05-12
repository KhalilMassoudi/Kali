package kali.microservices.billingservice.controller;

import kali.microservices.billingservice.entities.Invoice;
import kali.microservices.billingservice.entities.Subscription;
import kali.microservices.billingservice.service.InvoiceService;
import kali.microservices.billingservice.service.SubscriptionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/billing")
@RequiredArgsConstructor
public class BillingController {

    private final InvoiceService invoiceService;
    private final SubscriptionService subscriptionService;

    // ======================== INVOICES ========================

    @GetMapping("/invoices/user/{userId}")
    public ResponseEntity<List<Invoice>> getInvoices(@PathVariable Long userId) {
        return ResponseEntity.ok(invoiceService.getInvoicesByUser(userId));
    }

    @GetMapping("/invoices/{id}")
    public ResponseEntity<Invoice> getInvoice(@PathVariable Long id) {
        return ResponseEntity.ok(invoiceService.getInvoiceById(id));
    }

    @PostMapping("/invoices/{id}/pay")
    public ResponseEntity<Invoice> payInvoice(@PathVariable Long id) {
        return ResponseEntity.ok(invoiceService.payInvoice(id));
    }

    // ======================== SUBSCRIPTIONS ========================

    @PostMapping("/subscriptions/user/{userId}/subscribe")
    public ResponseEntity<Subscription> subscribe(@PathVariable Long userId,
                                                   @RequestParam Subscription.Plan plan) {
        return ResponseEntity.ok(subscriptionService.subscribe(userId, plan));
    }

    @GetMapping("/subscriptions/user/{userId}/active")
    public ResponseEntity<Subscription> getActiveSubscription(@PathVariable Long userId) {
        return ResponseEntity.ok(subscriptionService.getActiveSubscription(userId));
    }

    @GetMapping("/subscriptions/user/{userId}/history")
    public ResponseEntity<List<Subscription>> getSubscriptionHistory(@PathVariable Long userId) {
        return ResponseEntity.ok(subscriptionService.getSubscriptionHistory(userId));
    }

    @PostMapping("/subscriptions/user/{userId}/cancel")
    public ResponseEntity<Subscription> cancelSubscription(@PathVariable Long userId) {
        return ResponseEntity.ok(subscriptionService.cancelSubscription(userId));
    }

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Billing Service is running!");
    }
}