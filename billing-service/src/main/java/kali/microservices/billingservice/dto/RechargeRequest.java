package kali.microservices.billingservice.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class RechargeRequest {
    @NotNull
    @DecimalMin(value = "0.01", message = "Le montant doit être positif")
    private BigDecimal amount;
}
