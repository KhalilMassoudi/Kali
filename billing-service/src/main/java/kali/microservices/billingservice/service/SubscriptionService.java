package kali.microservices.billingservice.service;

import kali.microservices.billingservice.entities.Subscription;
import kali.microservices.billingservice.repository.SubscriptionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class SubscriptionService {

    private final SubscriptionRepository subscriptionRepository;

    // Prix des plans en TND/mois
    private static final Map<Subscription.Plan, BigDecimal> PLAN_PRICES = Map.of(
            Subscription.Plan.STARTER, new BigDecimal("29.99"),
            Subscription.Plan.PROFESSIONAL, new BigDecimal("99.99"),
            Subscription.Plan.ENTERPRISE, new BigDecimal("299.99")
    );

    public Subscription subscribe(Long userId, Subscription.Plan plan) {
        // Annuler l'abonnement actif existant s'il y en a un
        subscriptionRepository.findByUserIdAndStatus(userId, Subscription.SubscriptionStatus.ACTIVE)
                .ifPresent(existing -> {
                    existing.setStatus(Subscription.SubscriptionStatus.CANCELLED);
                    subscriptionRepository.save(existing);
                });

        Subscription subscription = new Subscription();
        subscription.setUserId(userId);
        subscription.setPlan(plan);
        subscription.setMonthlyPrice(PLAN_PRICES.get(plan));
        subscription.setStatus(Subscription.SubscriptionStatus.ACTIVE);
        subscription.setStartDate(LocalDate.now());
        subscription.setNextBillingDate(LocalDate.now().plusMonths(1));

        return subscriptionRepository.save(subscription);
    }

    public Subscription getActiveSubscription(Long userId) {
        return subscriptionRepository.findByUserIdAndStatus(userId, Subscription.SubscriptionStatus.ACTIVE)
                .orElseThrow(() -> new RuntimeException("Aucun abonnement actif pour l'utilisateur: " + userId));
    }

    public List<Subscription> getSubscriptionHistory(Long userId) {
        return subscriptionRepository.findByUserId(userId);
    }

    public Subscription cancelSubscription(Long userId) {
        Subscription subscription = getActiveSubscription(userId);
        subscription.setStatus(Subscription.SubscriptionStatus.CANCELLED);
        subscription.setEndDate(LocalDate.now());
        return subscriptionRepository.save(subscription);
    }
}