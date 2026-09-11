package kali.microservices.infrastructureservice.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import kali.microservices.infrastructureservice.entities.VpsBackupSchedule;
import lombok.Data;

@Data
public class BackupScheduleRequest {
    @NotNull private VpsBackupSchedule.Frequency frequency;
    @NotNull @Min(1) private Integer retentionCount;
    @NotNull private Boolean enabled;
}
