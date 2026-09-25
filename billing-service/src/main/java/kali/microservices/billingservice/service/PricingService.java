package kali.microservices.billingservice.service;

import kali.microservices.billingservice.entities.PricingConfig;
import kali.microservices.billingservice.repository.PricingConfigRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class PricingService {

    public static final BigDecimal DEFAULT_LOW_BALANCE_THRESHOLD = new BigDecimal("5.00");

    private final PricingConfigRepository repository;

    @Transactional
    public PricingConfig getConfig() {
        PricingConfig config = repository.findById(1L).orElseGet(() -> {
            PricingConfig created = new PricingConfig();
            created.setId(1L);
            return repository.save(created);
        });
        // The alert settings were added after the singleton row existed - backfill them once.
        if (config.getLowBalanceThreshold() == null || config.getSuspendVmsOnZeroBalance() == null) {
            if (config.getLowBalanceThreshold() == null) config.setLowBalanceThreshold(DEFAULT_LOW_BALANCE_THRESHOLD);
            if (config.getSuspendVmsOnZeroBalance() == null) config.setSuspendVmsOnZeroBalance(false);
            config = repository.save(config);
        }
        return config;
    }

    @Transactional
    public PricingConfig updateConfig(PricingConfig update) {
        PricingConfig config = getConfig();
        config.setPricePerVcpuHour(update.getPricePerVcpuHour());
        config.setPricePerRamGbHour(update.getPricePerRamGbHour());
        config.setPricePerStorageGbHour(update.getPricePerStorageGbHour());
        config.setPricePerFloatingIpHour(update.getPricePerFloatingIpHour());
        if (update.getCurrency() != null) config.setCurrency(update.getCurrency());
        if (update.getLowBalanceThreshold() != null) config.setLowBalanceThreshold(update.getLowBalanceThreshold());
        if (update.getSuspendVmsOnZeroBalance() != null) config.setSuspendVmsOnZeroBalance(update.getSuspendVmsOnZeroBalance());
        return repository.save(config);
    }

    public BigDecimal lowBalanceThreshold() {
        return getConfig().getLowBalanceThreshold();
    }

    public boolean suspendVmsOnZeroBalance() {
        return Boolean.TRUE.equals(getConfig().getSuspendVmsOnZeroBalance());
    }
}
