package kali.microservices.billingservice.repository;

import kali.microservices.billingservice.entities.PricingConfig;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PricingConfigRepository extends JpaRepository<PricingConfig, Long> {
}
