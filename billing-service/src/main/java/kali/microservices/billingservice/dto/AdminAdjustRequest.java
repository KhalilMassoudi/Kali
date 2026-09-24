package kali.microservices.billingservice.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class AdminAdjustRequest {
    @NotNull
    private BigDecimal amount; // positive = credit, negative = debit

    private String description;
}
