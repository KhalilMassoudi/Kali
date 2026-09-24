package kali.microservices.billingservice.service;

import kali.microservices.billingservice.entities.PricingConfig;
import kali.microservices.billingservice.repository.PricingConfigRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PricingService {

    private final PricingConfigRepository repository;

    @Transactional
    public PricingConfig getConfig() {
        return repository.findById(1L).orElseGet(() -> {
            PricingConfig config = new PricingConfig();
            config.setId(1L);
            return repository.save(config);
        });
    }

    @Transactional
    public PricingConfig updateConfig(PricingConfig update) {
        PricingConfig config = getConfig();
        config.setPricePerVcpuHour(update.getPricePerVcpuHour());
        config.setPricePerRamGbHour(update.getPricePerRamGbHour());
        config.setPricePerStorageGbHour(update.getPricePerStorageGbHour());
        config.setPricePerFloatingIpHour(update.getPricePerFloatingIpHour());
        if (update.getCurrency() != null) config.setCurrency(update.getCurrency());
        return repository.save(config);
    }
}
