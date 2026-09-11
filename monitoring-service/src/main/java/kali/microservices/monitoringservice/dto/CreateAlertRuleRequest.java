package kali.microservices.monitoringservice.dto;

import jakarta.validation.constraints.NotNull;
import kali.microservices.monitoringservice.entities.AlertRule;
import lombok.Data;

@Data
public class CreateAlertRuleRequest {

    /** null = applies to every service. */
    private String serviceName;

    @NotNull
    private AlertRule.AlertType alertType;

    @NotNull
    private Double threshold;
}
